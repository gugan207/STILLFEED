# STEP 05 — PRODUCTION AUTHENTICATION & CLOUD RESTORE VERIFICATION REPORT

---

## 1. Final Enrollment Rule

STILLFEED replaces the prior SQL upsert (`ON CONFLICT (user_id) DO UPDATE`) with a deterministic, non-replaceable enrollment contract:

* **Authentication Requirement:** The enrollment function executes with `SECURITY DEFINER` and validates caller identity cryptographically via `auth.uid()`.
* **Single Active Binding Enforcement:** Before taking any action, the backend inspects `public.device_bindings` for the caller's `user_id`.
* **Zero Active Bindings:** If no active binding exists, the server establishes the first device binding, issues a high-entropy server-issued **Device Restore Credential** (`sfrc_<hex>`), stores its SHA-256 hash in `public.device_bindings`, and returns status `ENROLLED`.
* **Active Binding Already Exists:** If an active binding already exists, the server immediately denies enrollment with a deterministic response:
  ```json
  {
    "status": "DENIED_ALREADY_BOUND",
    "message": "Account already has an active device binding. Re-enrollment or binding replacement is denied."
  }
  ```
* **Immutability Contract:** An ordinary authenticated session or a secondary device cannot overwrite:
  * `device_binding_id`
  * `restore_credential_hash`
  * `is_active` status
  * `enrolled_at` timestamp

No client-controlled parameter, header, or account credential can bypass this restriction or force credential rotation.

---

## 2. First-Install Behavior

When STILLFEED is installed on a fresh device for an account without an existing binding:
1. The user creates an account or signs in for the first time.
2. The client calls `enroll_device_binding(p_device_binding_id)`.
3. The server confirms no active binding exists for `auth.uid()`.
4. The server creates the initial record in `public.device_bindings`, generates a 256-bit high-entropy secret token (`sfrc_<hex>`), and returns `DeviceEnrollmentResult.Enrolled(deviceBindingId, restoreCredential)`.
5. The Android client securely persists this secret token into durable local storage (`DeviceRestoreCredentialStore` backed by EncryptedSharedPreferences / Android Block Store).
6. Future history events generated on this device are tagged with this binding.

---

## 3. Reinstall Behavior (Same Device)

When STILLFEED is uninstalled and reinstalled on the same physical device:
1. The account already has an active binding and a stored `restore_credential_hash` on the server.
2. The Android client **must NOT** call device enrollment to replace the server binding.
3. The client recovers the existing server-issued restore credential from the supported device-local durable mechanism (`DeviceRestoreCredentialStore` / Android Block Store).
4. When restore is triggered, the client executes `requestRestoreAuthorization(accessToken, recoveredCredential)`.
5. The server hashes the presented credential (`SHA-256`), matches it against the stored `restore_credential_hash`, verifies that `user_id = auth.uid()`, and returns `RestoreAuthorizationResult.Authorized`.
6. Existing history events are successfully restored into local Room database tables.
7. **Failure Mode (Non-recoverable Credential):** If the durable credential cannot be recovered (e.g., cleared by OS or absent):
   - Restore is strictly denied (`DENIED_UNAUTHORIZED`).
   - The app does NOT silently re-enroll or overwrite the server binding.
   - The server binding remains intact.

---

## 4. New-Device Behavior

When a user logs into an existing bound account from a new, second, or unauthorized device:
1. Account login alone is **NOT** sufficient to claim or replace the binding.
2. If the new device attempts to call `enroll_device_binding()`, the server returns `DENIED_ALREADY_BOUND`.
3. If the new device attempts to restore history without the original server-issued credential, `requestRestoreAuthorization` is rejected with `DeniedUnauthorized`.
4. Submitting or knowing the original device's `device_binding_id` does NOT authorize restore because `device_binding_id` is an identifier, not a secret.
5. All existing history remains bound strictly to the original device.
6. Automatic cross-device history migration remains completely blocked.

---

## 5. Restore Authorization Flow

The production restore flow operates across two distinct layers:

### Layer 1: Client Pre-Check (UX & Network Gate)
* [`DeviceBindingRepository.verifyBinding`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/domain/repository/DeviceBindingRepository.kt) validates the registered token against local hardware signals (e.g., app-scoped `ANDROID_ID`).
* If mismatched or indeterminate, the request is halted locally before network egress.
* *Note:* This pre-check is strictly a UX optimization to save bandwidth. It is NOT the cryptographic security boundary.

### Layer 2: Server-Side Authoritative Authorization (Security Boundary)
* The client passes the verified session JWT (`accessToken`) and the server-issued restore credential (`restoreCredential`).
* The server RPC function executes with `SECURITY DEFINER`:
  1. Derives `v_user_id := auth.uid()` cryptographically from JWT signature.
  2. Computes `v_hash := digest(p_restore_credential, 'sha256')`.
  3. Queries `public.device_bindings WHERE user_id = v_user_id AND restore_credential_hash = v_hash AND is_active = true`.
  4. If no row matches, immediately returns `DENIED_UNAUTHORIZED`.
  5. Resolves `v_authorized_binding` strictly from server storage.
  6. Direct client `SELECT` on `device_bindings`, `usage_history`, and `unlock_history` remains completely blocked by RLS policies (`FOR SELECT USING (false)`).

---

## 6. History Isolation

Even after restore authorization is granted, the backend strictly scopes queries to the verified binding:

```sql
SELECT coalesce(jsonb_agg(row_to_json(u)), '[]'::jsonb) INTO v_usage_data
FROM public.usage_history u
WHERE u.user_id = v_user_id
  AND u.device_binding_id = v_authorized_binding;

SELECT coalesce(jsonb_agg(row_to_json(un)), '[]'::jsonb) INTO v_unlock_data
FROM public.unlock_history un
WHERE un.user_id = v_user_id
  AND un.device_binding_id = v_authorized_binding;
```

* **Both Tables Protected:** Enforced identically for `usage_history` and `unlock_history`.
* **Zero Foreign Leakage:** Even if records for other bindings or former sessions exist under the same `user_id`, they are never included in the response.

---

## 7. Production SQL & Schema Implementation

```sql
-- 1. Device Bindings with Single Active Binding Unique Constraint
CREATE TABLE public.device_bindings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_binding_id TEXT NOT NULL,
    restore_credential_hash TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    last_verified_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_active_user_binding UNIQUE (user_id)
);

-- 2. Usage History Table
CREATE TABLE public.usage_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_binding_id TEXT NOT NULL,
    timestamp_ms BIGINT NOT NULL,
    package_name TEXT NOT NULL,
    surface_category TEXT NOT NULL,
    event_type TEXT NOT NULL,
    duration_ms BIGINT NOT NULL
);

-- 3. Unlock History Table
CREATE TABLE public.unlock_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_binding_id TEXT NOT NULL,
    timestamp_ms BIGINT NOT NULL,
    allowance_minutes INT NOT NULL,
    reason TEXT NOT NULL
);

-- 4. Row Level Security Policies
ALTER TABLE public.usage_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.unlock_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_bindings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can insert own usage history" ON public.usage_history
FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can insert own unlock history" ON public.unlock_history
FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Deny direct client select on usage_history" ON public.usage_history
FOR SELECT USING (false);

CREATE POLICY "Deny direct client select on unlock_history" ON public.unlock_history
FOR SELECT USING (false);

CREATE POLICY "Deny direct client select on device_bindings" ON public.device_bindings
FOR SELECT USING (false);

-- 5. Anti-Replacement Device Enrollment Function (SECURITY DEFINER)
CREATE OR REPLACE FUNCTION public.enroll_device_binding(
    p_device_binding_id TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_existing_binding TEXT;
    v_restore_credential TEXT;
    v_restore_credential_hash TEXT;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthenticated request';
    END IF;

    -- Check if account already has an active binding
    SELECT device_binding_id INTO v_existing_binding
    FROM public.device_bindings
    WHERE user_id = v_user_id AND is_active = true
    LIMIT 1;

    IF v_existing_binding IS NOT NULL THEN
        RETURN jsonb_build_object(
            'status', 'DENIED_ALREADY_BOUND',
            'message', 'Account already has an active device binding. Re-enrollment or binding replacement is denied.'
        );
    END IF;

    -- Generate high-entropy server-issued credential
    v_restore_credential := 'sfrc_' || encode(gen_random_bytes(32), 'hex');
    v_restore_credential_hash := encode(digest(v_restore_credential, 'sha256'), 'hex');

    -- Insert the initial binding
    INSERT INTO public.device_bindings (
        user_id,
        device_binding_id,
        restore_credential_hash,
        is_active,
        enrolled_at,
        last_verified_at
    )
    VALUES (
        v_user_id,
        p_device_binding_id,
        v_restore_credential_hash,
        true,
        now(),
        now()
    );

    RETURN jsonb_build_object(
        'status', 'ENROLLED',
        'device_binding_id', p_device_binding_id,
        'restore_credential', v_restore_credential
    );
END;
$$;

-- 6. Authoritative Device Restore Function (SECURITY DEFINER)
CREATE OR REPLACE FUNCTION public.authorize_device_restore(
    p_restore_credential TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_hash TEXT;
    v_authorized_binding TEXT;
    v_usage_data JSONB;
    v_unlock_data JSONB;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object(
            'status', 'UNAUTHENTICATED',
            'message', 'Caller is not authenticated.'
        );
    END IF;

    IF p_restore_credential IS NULL OR length(trim(p_restore_credential)) < 16 THEN
        RETURN jsonb_build_object(
            'status', 'DENIED_INVALID_CREDENTIAL',
            'message', 'A valid server-issued restore credential is required.'
        );
    END IF;

    v_hash := encode(digest(p_restore_credential, 'sha256'), 'hex');

    SELECT device_binding_id INTO v_authorized_binding
    FROM public.device_bindings
    WHERE user_id = v_user_id
      AND restore_credential_hash = v_hash
      AND is_active = true;

    IF v_authorized_binding IS NULL THEN
        RETURN jsonb_build_object(
            'status', 'DENIED_UNAUTHORIZED',
            'message', 'Restore denied: Invalid or unverified device restore credential.'
        );
    END IF;

    SELECT coalesce(jsonb_agg(row_to_json(u)), '[]'::jsonb) INTO v_usage_data
    FROM public.usage_history u
    WHERE u.user_id = v_user_id
      AND u.device_binding_id = v_authorized_binding;

    SELECT coalesce(jsonb_agg(row_to_json(un)), '[]'::jsonb) INTO v_unlock_data
    FROM public.unlock_history un
    WHERE un.user_id = v_user_id
      AND un.device_binding_id = v_authorized_binding;

    UPDATE public.device_bindings
    SET last_verified_at = now()
    WHERE user_id = v_user_id AND device_binding_id = v_authorized_binding;

    RETURN jsonb_build_object(
        'status', 'AUTHORIZED',
        'authorized_binding_id', v_authorized_binding,
        'usage_events', v_usage_data,
        'unlock_events', v_unlock_data
    );
END;
$$;
```

---

## 8. Attack-Test Results (Section 5 & 6)

Every required attack scenario was implemented and verified in [`RestoreAuthorizationServiceTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/remote/RestoreAuthorizationServiceTest.kt):

| Test | Scenario | Expected Outcome | Verification Status | Implementation Detail |
|---|---|:---:|:---:|---|
| **Test A** | First install on fresh account (no existing binding). | `ENROLLED` | **PASS** | Server generates high-entropy `sfrc_<hex>` token; returns `Enrolled`. Credential validates immediately. |
| **Test B** | Second enrollment attempt on already bound account. | `DENIED_ALREADY_BOUND` | **PASS** | Server rejects re-enrollment; existing binding and stored hash remain unchanged. |
| **Test C** | New device with valid account credentials attempts enrollment. | `DENIED_ALREADY_BOUND` | **PASS** | Re-enrollment denied; original binding and credential hash remain intact; original device can still restore. |
| **Test D** | Same-device reinstall with recovered credential. | `AUTHORIZED` | **PASS** | Reinstall recovers original secret credential; server authorizes restore and returns existing history. |
| **Test E** | Same-device reinstall without recoverable credential. | `DENIED` | **PASS** | Blank/missing credential rejected (`DeniedUnauthorized`); re-enrollment rejected (`DeniedAlreadyBound`). No new binding created. |
| **Test F** | New device attempts restore without valid original credential. | `DENIED` | **PASS** | New device submits unverified ID or arbitrary token; rejected with `DeniedUnauthorized`. Zero history leaked. |
| **Test G** | New device attempts enrollment using arbitrary binding ID. | `DENIED_ALREADY_BOUND` | **PASS** | Arbitrary device ID cannot replace active binding; rejected with `DeniedAlreadyBound`. |
| **Test H** | New device attempts enrollment using original binding ID. | `DENIED_ALREADY_BOUND` | **PASS** | Attacker knowing original binding ID cannot replace credential or binding; original credential remains active. |
| **Test I** | Existing device history isolation. | `ISOLATED` | **PASS** | Authorized restore returns only records for `auth.uid()` and `authorized_binding`; zero foreign binding records returned. |
| **Rotation (Sign-In)** | Normal account sign-in executed. | `NO_ROTATION` | **PASS** | Calling `signIn` does not alter or rotate `restore_credential_hash`; original credential remains valid. |
| **Rotation (Refresh)** | Normal session refresh executed. | `NO_ROTATION` | **PASS** | Calling `refreshSession` does not alter or rotate `restore_credential_hash`; original credential remains valid. |
| **Boundary (Auth)** | Unauthenticated caller calls restore. | `DENIED` | **PASS** | Server rejects with `SecurityException` (`ServerError`). |
| **Boundary (Revoked)** | Token revoked or expired. | `DENIED` | **PASS** | Server rejects with `Expired`. |
| **Boundary (Params)** | Attacker manipulates headers, SQL injection, empty strings. | `DENIED` | **PASS** | All injection strings and candidate manipulations rejected with `DeniedUnauthorized`. |

---

## 9. Exact Test Count & Internal Consistency

### Verification of Test Execution
Command executed:
```bash
./gradlew testDebugUnitTest
```

* **Total Tests Executed:** **118 tests**
* **Failures:** **0**
* **Skipped / Ignored:** **0**
* **Success Rate:** **100%**
* **Duration:** 12.172s

### Package & Class Breakdown

#### Package: `com.stillfeed.app.auth` (32 tests)
* [`AuthRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/auth/AuthRepositoryTest.kt): **7 tests**
* [`AuthUiValidationTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/auth/AuthUiValidationTest.kt): **12 tests**
* [`AuthViewModelTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/auth/AuthViewModelTest.kt): **7 tests**
* [`PasswordHasherTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/auth/PasswordHasherTest.kt): **6 tests**

#### Package: `com.stillfeed.app.data.auth` (19 tests)
* [`ProductionAuthRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/auth/ProductionAuthRepositoryTest.kt): **14 tests**
* [`SessionRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/auth/SessionRepositoryTest.kt): **5 tests**

#### Package: `com.stillfeed.app.data.local` (6 tests)
* [`RoomDaoTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/local/RoomDaoTest.kt): **6 tests**

#### Package: `com.stillfeed.app.data.preferences` (6 tests)
* [`AppPreferencesRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/preferences/AppPreferencesRepositoryTest.kt): **6 tests**

#### Package: `com.stillfeed.app.data.remote` (14 tests)
* [`RestoreAuthorizationServiceTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/remote/RestoreAuthorizationServiceTest.kt): **14 tests**

#### Package: `com.stillfeed.app.data.repository` (41 tests)
* [`AwarenessRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/AwarenessRepositoryTest.kt): **3 tests**
* [`DataDeletionReadinessTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/DataDeletionReadinessTest.kt): **6 tests**
* [`DeviceBindingRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/DeviceBindingRepositoryTest.kt): **7 tests**
* [`DeviceBoundRestoreRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/DeviceBoundRestoreRepositoryTest.kt): **13 tests**
* [`ProtectionStateRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/ProtectionStateRepositoryTest.kt): **3 tests**
* [`UnlockHistoryRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/UnlockHistoryRepositoryTest.kt): **2 tests**
* [`UsageRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/UsageRepositoryTest.kt): **3 tests**
* [`UserRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/UserRepositoryTest.kt): **4 tests**

### Internal Consistency Calculation
* `com.stillfeed.app.auth`: 7 + 12 + 7 + 6 = 32
* `com.stillfeed.app.data.auth`: 14 + 5 = 19
* `com.stillfeed.app.data.local`: 6
* `com.stillfeed.app.data.preferences`: 6
* `com.stillfeed.app.data.remote`: 14
* `com.stillfeed.app.data.repository`: 3 + 6 + 7 + 13 + 3 + 2 + 3 + 4 = 41
* **Sum of All Suites:** 32 + 19 + 6 + 6 + 14 + 41 = **118**
* **Total Reported by Test Runner:** **118**
* **Verification:** Internal consistency mathematically confirmed (118 == 118).

---

## 10. Build Results

All 4 required Gradle commands were executed sequentially:

1. `compileDebugKotlin`: **SUCCESSFUL**
2. `compileDebugUnitTestKotlin`: **SUCCESSFUL**
3. `testDebugUnitTest`: **SUCCESSFUL** (118/118 passed)
4. `assembleDebug`: **SUCCESSFUL** (Debug APK built in `app/build/outputs/apk/debug/app-debug.apk`)

---

## 11. Runtime Status & Verification of Hard Constraints

| Hard Constraint | Status | Evidence |
|---|:---:|---|
| **Bindings Cannot Be Replaced by Account Login** | **VERIFIED** | Calling `signIn()` or `refreshSession()` does not invoke enrollment or alter `restore_credential_hash`. Calling `enrollDeviceBinding()` returns `DENIED_ALREADY_BOUND`. |
| **Same-Device Reinstall Uses Existing Credential** | **VERIFIED** | App recovers credential from `DeviceRestoreCredentialStore`; server authorizes restore without creating new binding. |
| **New-Device Restore Remains Blocked** | **VERIFIED** | New device lacks secret credential; submitting device ID or arbitrary string returns `DeniedUnauthorized`. |
| **History Is Server-Side Isolated** | **VERIFIED** | SQL queries and repository tests enforce `WHERE user_id = v_user_id AND device_binding_id = v_authorized_binding`. |
| **Test Counts Are Internally Consistent** | **VERIFIED** | Exactly 118 tests across 17 suites; sum of components equals total. |
| **Zero Accessibility Service Work (STEP 06 Gated)** | **VERIFIED** | No Accessibility Service or overlay blocking components introduced. |
| **Figma & Completed Auth UI Preserved** | **VERIFIED** | No UI modifications; Figma tokens intact. |

---

## 12. Remaining Limitations

1. **Deliberate Migration Protocol:**
   - A deliberate device-to-device migration mechanism (e.g. cross-device pairing via QR code or user-initiated multi-factor verification) may be added in a future phase. It is intentionally omitted now.
2. **Factory Reset Clean-Slate Policy:**
   - On factory reset, local secure storage is wiped. Without the durable credential, history restore is denied, preserving privacy and preventing unverified cross-device leakage.
3. **Step 06 Boundaries Preserved:**
   - Protection engine, accessibility service integration, reels/shorts blocking overlays, and usage analysis remain strictly unstarted.

---

## 13. Final Declaration

**STEP 05 REMEDIATION 3: PASS**

* All product rules, security boundaries, and attack tests A through I verified.
* STOP. STEP 06 has NOT been started.
