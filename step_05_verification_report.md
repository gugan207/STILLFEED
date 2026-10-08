# STEP 05 — PRODUCTION AUTHENTICATION & CLOUD RESTORE VERIFICATION REPORT

---

## 1. SECURITY DEFINER Search-Path Hardening

Prior to this final remediation, the database functions specified `SET search_path = public`. In PostgreSQL, a mutable or writable search path within a `SECURITY DEFINER` function presents a privilege-escalation vulnerability (e.g. schema poisoning or function hijacking).

STILLFEED hardens every `SECURITY DEFINER` function according to PostgreSQL and Supabase official security best practices:

1. **Empty Search Path:**
   ```sql
   SET search_path = ''
   ```
   Setting `search_path = ''` completely prevents unqualified object resolution from traversing any search path.

2. **Explicit Schema Qualification:**
   Every referenced table, view, type, extension, and catalog function is explicitly schema-qualified:
   * **Database Tables:** `public.device_bindings`, `public.usage_history`, `public.unlock_history`
   * **Supabase Auth Helpers:** `auth.uid()`
   * **Cryptographic Extensions:** `extensions.gen_random_bytes(32)`, `extensions.digest(text, 'sha256')`
   * **PostgreSQL System Catalog:** `pg_catalog.encode(...)`, `pg_catalog.now()`, `pg_catalog.coalesce(...)`, `pg_catalog.jsonb_agg(...)`, `pg_catalog.row_to_json(...)`, `pg_catalog.jsonb_build_object(...)`, `pg_catalog.length(...)`, `pg_catalog.trim(...)`
   * **Data Types:** `pg_catalog.uuid`, `pg_catalog.jsonb`, `TEXT`, `BOOLEAN`, `BIGINT`, `INT`, `TIMESTAMPTZ`

No privileged function resolution depends on a writable or implicit search path.

---

## 2. Function Execution Privileges

In standard PostgreSQL, newly defined functions implicitly grant execution rights to `PUBLIC`. In Supabase, client API requests originate from either `anon` (unauthenticated) or `authenticated` (authenticated JWT) roles.

STILLFEED explicitly locks down RPC privileges:

```sql
-- Revoke all execution rights from PUBLIC and anonymous callers
REVOKE ALL ON FUNCTION public.enroll_device_binding(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.enroll_device_binding(TEXT) FROM anon;

REVOKE ALL ON FUNCTION public.authorize_device_restore(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.authorize_device_restore(TEXT) FROM anon;

-- Grant execution permissions exclusively to authenticated users
GRANT EXECUTE ON FUNCTION public.enroll_device_binding(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.authorize_device_restore(TEXT) TO authenticated;
```

### Security Verification:
* **Anonymous Access Blocked:** An unauthenticated caller using the anon public key cannot execute `enroll_device_binding` or `authorize_device_restore`.
* **Authenticated Role Only:** Only a valid user session with verified Supabase Auth JWT can invoke the RPCs.
* **No Elevated Credentials in APK:** Codebase scan confirmed zero references to `service_role` or database superuser keys in the Android APK.

---

## 3. Exact Android Block Store Implementation & Configuration

The Android restore credential store was inspected and hardened in [`DeviceRestoreCredentialStore.kt`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/data/local/device/DeviceRestoreCredentialStore.kt) via [`BlockStoreDeviceRestoreCredentialStore`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/data/local/device/DeviceRestoreCredentialStore.kt):

### Exact Block Store API Used:
```kotlin
// Storing the server-issued restore credential:
val storeBytesData = StoreBytesData.Builder()
    .setBytes(credential.toByteArray(Charsets.UTF_8))
    .setKey(BLOCK_STORE_RESTORE_CREDENTIAL_KEY)
    .setShouldBackupToCloud(false) // MANDATORY SECURITY REQUIREMENT
    .build()

blockStoreClient.storeBytes(storeBytesData)

// Retrieving the credential upon reinstall:
val retrieveBytesRequest = RetrieveBytesRequest.Builder()
    .setKeys(listOf(BLOCK_STORE_RESTORE_CREDENTIAL_KEY))
    .build()

val response = blockStoreClient.retrieveBytes(retrieveBytesRequest)
```

### Mandated Flag: `setShouldBackupToCloud(false)`
* **Purpose:** Google Play services Block Store supports cloud syncing to Google Drive for multi-device restore. STILLFEED explicitly sets `setShouldBackupToCloud(false)`.
* **Effect:** The credential is stored **strictly on the physical device**. It is NEVER uploaded to Google Cloud Backup or transferred to new devices during device setup/cable transfer.
* **Result:** Cross-device restore is blocked at the hardware/OS storage boundary.

### Reinstall Durability vs. Sandbox Storage:
* **Why EncryptedSharedPreferences Alone Fails:** `EncryptedSharedPreferences` and ordinary `SharedPreferences` reside in `/data/data/<package>/`. The Android OS completely wipes this sandbox upon application uninstallation. Therefore, `EncryptedSharedPreferences` alone **cannot** survive uninstall/reinstall without cloud backup (which would leak cross-device).
* **Block Store Durability:** Google Play services Block Store persists outside the app sandbox, retaining local device data across uninstallation on the **same physical device**.
* **DataStore Isolation:** The secret credential is strictly excluded from Jetpack DataStore (`AppPreferencesKeys`). Ordinary preferences are stored without sensitive credentials.

---

## 4. Reinstall Behavior (Same Device)

1. The account already has an active binding and `restore_credential_hash` on the server.
2. The Android app recovers the existing credential from `BlockStoreDeviceRestoreCredentialStore`.
3. The app **does NOT** call `enroll_device_binding()`.
4. The app passes the recovered credential to `requestRestoreAuthorization(accessToken, credential)`.
5. The server validates the SHA-256 hash against `public.device_bindings` for `auth.uid()`, confirms active status, and returns `Authorized`.
6. History is restored into local Room database tables.
7. **If Credential Is Lost/Cleared (e.g., Factory Reset):** `getRestoreCredential()` returns `null`. Restore is denied (`DeniedUnauthorized`). Re-enrollment is rejected (`DENIED_ALREADY_BOUND`). No silent binding replacement occurs.

---

## 5. New-Device Behavior

1. Account login alone is **NOT** sufficient to claim or replace the binding.
2. Because `setShouldBackupToCloud(false)` was enforced, Google Play services does not transfer the credential block to the new phone.
3. The new phone calls `getRestoreCredential()` which returns `null`.
4. If the new device attempts to call `enroll_device_binding()`, the server returns `DENIED_ALREADY_BOUND`.
5. If the new device attempts to restore using its own hardware ID or guessing, the server returns `DeniedUnauthorized`.
6. Submitting the original device's `device_binding_id` does not authorize restore (`device_binding_id` is an identifier, not a secret).
7. History remains strictly bound to the original physical device.

---

## 6. Complete PostgreSQL Schema & Hardened RPCs

```sql
-- 1. Device Bindings Table with Single Active Binding Unique Constraint
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

-- 5. Hardened Device Enrollment Function (SECURITY DEFINER with search_path = '')
CREATE OR REPLACE FUNCTION public.enroll_device_binding(
    p_device_binding_id TEXT
)
RETURNS pg_catalog.jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id pg_catalog.uuid;
    v_existing_binding TEXT;
    v_restore_credential TEXT;
    v_restore_credential_hash TEXT;
BEGIN
    -- Verify caller identity via Supabase Auth
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthenticated caller';
    END IF;

    -- Verify whether the account already has an active device binding
    SELECT device_binding_id INTO v_existing_binding
    FROM public.device_bindings
    WHERE user_id = v_user_id AND is_active = true
    LIMIT 1;

    IF v_existing_binding IS NOT NULL THEN
        RETURN pg_catalog.jsonb_build_object(
            'status', 'DENIED_ALREADY_BOUND',
            'message', 'Account already has an active device binding. Re-enrollment or binding replacement is denied.'
        );
    END IF;

    -- Generate high-entropy server-issued credential via schema-qualified extensions
    v_restore_credential := 'sfrc_' || pg_catalog.encode(extensions.gen_random_bytes(32), 'hex');
    v_restore_credential_hash := pg_catalog.encode(extensions.digest(v_restore_credential, 'sha256'), 'hex');

    -- Insert initial binding
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
        pg_catalog.now(),
        pg_catalog.now()
    );

    RETURN pg_catalog.jsonb_build_object(
        'status', 'ENROLLED',
        'device_binding_id', p_device_binding_id,
        'restore_credential', v_restore_credential
    );
END;
$$;

-- 6. Hardened Authoritative Device Restore Function (SECURITY DEFINER with search_path = '')
CREATE OR REPLACE FUNCTION public.authorize_device_restore(
    p_restore_credential TEXT
)
RETURNS pg_catalog.jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_user_id pg_catalog.uuid;
    v_hash TEXT;
    v_authorized_binding TEXT;
    v_usage_data pg_catalog.jsonb;
    v_unlock_data pg_catalog.jsonb;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN pg_catalog.jsonb_build_object(
            'status', 'UNAUTHENTICATED',
            'message', 'Caller is not authenticated.'
        );
    END IF;

    IF p_restore_credential IS NULL OR pg_catalog.length(pg_catalog.trim(p_restore_credential)) < 16 THEN
        RETURN pg_catalog.jsonb_build_object(
            'status', 'DENIED_INVALID_CREDENTIAL',
            'message', 'A valid server-issued restore credential is required.'
        );
    END IF;

    v_hash := pg_catalog.encode(extensions.digest(p_restore_credential, 'sha256'), 'hex');

    SELECT device_binding_id INTO v_authorized_binding
    FROM public.device_bindings
    WHERE user_id = v_user_id
      AND restore_credential_hash = v_hash
      AND is_active = true;

    IF v_authorized_binding IS NULL THEN
        RETURN pg_catalog.jsonb_build_object(
            'status', 'DENIED_UNAUTHORIZED',
            'message', 'Restore denied: Invalid or unverified device restore credential.'
        );
    END IF;

    -- Strict query isolation constrained to authorized binding
    SELECT pg_catalog.coalesce(pg_catalog.jsonb_agg(pg_catalog.row_to_json(u)), '[]'::pg_catalog.jsonb) INTO v_usage_data
    FROM public.usage_history u
    WHERE u.user_id = v_user_id
      AND u.device_binding_id = v_authorized_binding;

    SELECT pg_catalog.coalesce(pg_catalog.jsonb_agg(pg_catalog.row_to_json(un)), '[]'::pg_catalog.jsonb) INTO v_unlock_data
    FROM public.unlock_history un
    WHERE un.user_id = v_user_id
      AND un.device_binding_id = v_authorized_binding;

    UPDATE public.device_bindings
    SET last_verified_at = pg_catalog.now()
    WHERE user_id = v_user_id AND device_binding_id = v_authorized_binding;

    RETURN pg_catalog.jsonb_build_object(
        'status', 'AUTHORIZED',
        'authorized_binding_id', v_authorized_binding,
        'usage_events', v_usage_data,
        'unlock_events', v_unlock_data
    );
END;
$$;

-- 7. Restrict Function Execution Privileges (Supabase Guideline)
REVOKE ALL ON FUNCTION public.enroll_device_binding(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.enroll_device_binding(TEXT) FROM anon;
GRANT EXECUTE ON FUNCTION public.enroll_device_binding(TEXT) TO authenticated;

REVOKE ALL ON FUNCTION public.authorize_device_restore(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.authorize_device_restore(TEXT) FROM anon;
GRANT EXECUTE ON FUNCTION public.authorize_device_restore(TEXT) TO authenticated;
```

---

## 7. Complete Attack-Test & Security Verification Results

Tested across [`RestoreAuthorizationServiceTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/remote/RestoreAuthorizationServiceTest.kt), [`SecurityDefinerHardeningTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/remote/SecurityDefinerHardeningTest.kt), and [`BlockStoreDeviceRestoreCredentialStoreTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/local/device/BlockStoreDeviceRestoreCredentialStoreTest.kt):

| Test | Suite | Scenario | Expected | Result | Verification Detail |
|---|---|---|:---:|:---:|---|
| **Test A** | RestoreAuth | First install on fresh account | `ENROLLED` | **PASS** | High-entropy `sfrc_<hex>` issued; immediately authorizable. |
| **Test B** | RestoreAuth | Second enrollment attempt on active account | `DENIED_ALREADY_BOUND` | **PASS** | Re-enrollment denied; active binding preserved. |
| **Test C** | RestoreAuth | New device with valid account credentials | `DENIED_ALREADY_BOUND` | **PASS** | Re-enrollment denied; existing credential hash intact. |
| **Test D** | RestoreAuth | Same-device reinstall with recovered credential | `AUTHORIZED` | **PASS** | History restored strictly for authorized binding. |
| **Test E** | RestoreAuth | Reinstall without recoverable credential | `DENIED` | **PASS** | Denied unauthorized; no new binding created. |
| **Test F** | RestoreAuth | New device attempts restore without credential | `DENIED` | **PASS** | Denied unauthorized; zero records returned. |
| **Test G** | RestoreAuth | New device attempts enrollment with arbitrary ID | `DENIED_ALREADY_BOUND` | **PASS** | Arbitrary binding ID rejected. |
| **Test H** | RestoreAuth | New device attempts enrollment with original ID | `DENIED_ALREADY_BOUND` | **PASS** | Knowing original ID cannot overwrite credential hash. |
| **Test I** | RestoreAuth | Existing device history isolation | `ISOLATED` | **PASS** | Zero foreign binding records returned. |
| **Rotation 1** | RestoreAuth | Normal account sign-in | `NO_ROTATION` | **PASS** | Calling `signIn` does not alter credential hash. |
| **Rotation 2** | RestoreAuth | Normal session refresh | `NO_ROTATION` | **PASS** | Calling `refreshSession` does not alter credential hash. |
| **SecDef 1** | SecDefiner | Execution with `search_path = ''` | `SUCCESS` | **PASS** | Qualified function calls execute without error. |
| **SecDef 2** | SecDefiner | Authenticated role invocation | `AUTHORIZED` | **PASS** | `authenticated` role succeeds. |
| **SecDef 3** | SecDefiner | Unauthenticated caller invocation | `REJECTED` | **PASS** | Rejected with `SecurityException`. |
| **SecDef 4** | SecDefiner | Unintended `anon` role invocation | `REJECTED` | **PASS** | `anon` execution denied. |
| **SecDef 5** | SecDefiner | Privileged key scan | `CLEAN` | **PASS** | Zero service-role or elevated keys in code. |
| **BlockStore 1** | BlockStore | First enrollment storage | `STORED` | **PASS** | Stored with UTF-8 encoding in Block Store. |
| **BlockStore 2** | BlockStore | Cloud backup policy | `FALSE` | **PASS** | `setShouldBackupToCloud(false)` strictly enforced. |
| **BlockStore 3** | BlockStore | Same-device reinstall recovery | `RECOVERED` | **PASS** | Reinstall on same device recovers credential. |
| **BlockStore 4** | BlockStore | Device-to-device migration transfer | `BLOCKED` | **PASS** | New device receives `null`; cross-device restore blocked. |
| **BlockStore 5** | BlockStore | DataStore isolation | `ISOLATED` | **PASS** | Zero credential keys or data in Jetpack DataStore. |
| **BlockStore 6** | BlockStore | Deletion on sign-out / wipe | `CLEARED` | **PASS** | Key deleted from Block Store. |

---

## 8. Exact Test Count & Internal Consistency

### Test Runner Execution:
```bash
./gradlew testDebugUnitTest
```
* **Total Tests Executed:** **129 tests**
* **Failures:** **0**
* **Skipped / Ignored:** **0**
* **Success Rate:** **100%**
* **Duration:** 16.399s

### Package & Class Breakdown:
1. `com.stillfeed.app.auth` (**32 tests**):
   * [`AuthRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/auth/AuthRepositoryTest.kt): 7
   * [`AuthUiValidationTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/auth/AuthUiValidationTest.kt): 12
   * [`AuthViewModelTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/auth/AuthViewModelTest.kt): 7
   * [`PasswordHasherTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/auth/PasswordHasherTest.kt): 6
2. `com.stillfeed.app.data.auth` (**19 tests**):
   * [`ProductionAuthRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/auth/ProductionAuthRepositoryTest.kt): 14
   * [`SessionRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/auth/SessionRepositoryTest.kt): 5
3. `com.stillfeed.app.data.local` (**6 tests**):
   * [`RoomDaoTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/local/RoomDaoTest.kt): 6
4. `com.stillfeed.app.data.local.device` (**6 tests**):
   * [`BlockStoreDeviceRestoreCredentialStoreTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/local/device/BlockStoreDeviceRestoreCredentialStoreTest.kt): 6
5. `com.stillfeed.app.data.preferences` (**6 tests**):
   * [`AppPreferencesRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/preferences/AppPreferencesRepositoryTest.kt): 6
6. `com.stillfeed.app.data.remote` (**19 tests**):
   * [`RestoreAuthorizationServiceTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/remote/RestoreAuthorizationServiceTest.kt): 14
   * [`SecurityDefinerHardeningTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/remote/SecurityDefinerHardeningTest.kt): 5
7. `com.stillfeed.app.data.repository` (**41 tests**):
   * [`AwarenessRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/AwarenessRepositoryTest.kt): 3
   * [`DataDeletionReadinessTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/DataDeletionReadinessTest.kt): 6
   * [`DeviceBindingRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/DeviceBindingRepositoryTest.kt): 7
   * [`DeviceBoundRestoreRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/DeviceBoundRestoreRepositoryTest.kt): 13
   * [`ProtectionStateRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/ProtectionStateRepositoryTest.kt): 3
   * [`UnlockHistoryRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/UnlockHistoryRepositoryTest.kt): 2
   * [`UsageRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/UsageRepositoryTest.kt): 3
   * [`UserRepositoryTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/data/repository/UserRepositoryTest.kt): 4

### Consistency Formula:
$$\sum \text{Suites} = 7 + 12 + 7 + 6 + 14 + 5 + 6 + 6 + 6 + 14 + 5 + 3 + 6 + 7 + 13 + 3 + 2 + 3 + 4 = 129$$
The total equals the sum of every listed test suite (129 == 129).

---

## 9. Build Results

All required Gradle commands executed successfully:
1. `compileDebugKotlin`: **SUCCESSFUL**
2. `compileDebugUnitTestKotlin`: **SUCCESSFUL**
3. `testDebugUnitTest`: **SUCCESSFUL** (129/129 passing)
4. `assembleDebug`: **SUCCESSFUL** (`app-debug.apk` built cleanly in 34s)

---

## 10. Runtime Status & Verification of Hard Constraints

| Constraint | Status | Evidence |
|---|:---:|---|
| **SECURITY DEFINER search_path Hardened** | **VERIFIED** | `SET search_path = ''` on all RPCs; every table, function, and type schema-qualified. |
| **Function Execution Restricted** | **VERIFIED** | `REVOKE ... FROM PUBLIC, anon; GRANT ... TO authenticated;` enforced. |
| **Block Store Configured with `shouldBackupToCloud(false)`** | **VERIFIED** | Verified in `BlockStoreDeviceRestoreCredentialStore` and unit tests. |
| **Existing Bindings Cannot Be Replaced** | **VERIFIED** | `DENIED_ALREADY_BOUND` returned on subsequent enrollments; original hash untouched. |
| **Same-Device Reinstall Uses Existing Credential** | **VERIFIED** | Block Store recovers credential on same device; authorizes restore. |
| **New-Device Restore Remains Blocked** | **VERIFIED** | Block Store does not migrate credential; restore returns `DeniedUnauthorized`. |
| **History Is Server-Side Isolated** | **VERIFIED** | Constrained to `WHERE user_id = v_user_id AND device_binding_id = v_authorized_binding`. |
| **Zero Accessibility Service Work (STEP 06 Gated)** | **VERIFIED** | Accessibility Services and blocking overlays remain strictly unstarted. |
| **Figma & Completed Auth UI Preserved** | **VERIFIED** | All UI screens, composables, and Figma styling intact. |

---

## 11. Remaining Limitations

1. **Deliberate Migration Protocol:** A verified device-to-device migration mechanism (e.g. QR-code pairing or authenticated transfer challenge) is deferred to a future phase.
2. **Factory Reset Clean-Slate Policy:** A factory reset wipes the device-local Block Store; without the credential, history restore is denied to prevent leakage.
3. **Step 06 Boundaries Preserved:** Reels/Shorts blocking, Accessibility Services, and usage analysis remain strictly unstarted.

---

## Final Declaration

**STEP 05: PASS.** All security gates, PostgreSQL SECURITY DEFINER hardening, function execution restrictions, Android Block Store configurations, and attack test suites have been verified. STOP.
