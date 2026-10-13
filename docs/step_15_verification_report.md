# STILLFEED — STEP 15 GOOGLE SIGN-IN DIAGNOSTICS & VERIFICATION REPORT
**Physical Device Test Analysis, Google Cloud Console Web Client ID Fix & Verification Report**

- **Repository:** [https://github.com/gugan207/STILLFEED](https://github.com/gugan207/STILLFEED)
- **Supabase Project ID:** `hrcazizzsbdmcnewowut`
- **Connected Physical Device:** Motorola Edge 60 Fusion (`ZA222WTN4H`, Android 16 / SDK 36)
- **Status:** **PENDING MANUAL VALIDATION** (Web Client ID updated to `92258811699-dj7fh1fc8lh460h1rtm1f8k0f20pdk4m.apps.googleusercontent.com`, APK reinstalled via `adb install -r`, awaiting user physical device sign-in; Email OTP: PASS)
- **Timestamp:** 2026-10-13T05:20:00+05:30

---

## 1. Executive Summary & Verification Matrix

| Authentication Mechanism | Verification Result | Root Cause / Current State |
| :--- | :--- | :--- |
| **Email OTP (6-Digit Numeric)** | **PASS** | Successfully verified with real Resend SMTP (`smtp.resend.com:587`) and Supabase GoTrue backend (`mailer_autoconfirm: false`). |
| **Google Sign-In (Credential Manager)** | **AWAITING USER MANUAL TEST** | Web Client ID updated to `92258811699-dj7fh1fc8lh460h1rtm1f8k0f20pdk4m.apps.googleusercontent.com`. Verified in Credential Manager. Non-destructively installed on device. |
| **Login UI & Ergonomics** | **PASS** | 100% Figma-approved design preserved intact. No UI regressions, zero status bar or navigation bar clipping. |
| **Non-Destructive Deployment** | **PASS** | Reinstalled via `adb install -r` without wiping user data or preferences. |

> [!CAUTION]
> **Strict Verification Policy:** As instructed, Google Sign-In is **NOT claimed as PASS** until real Google login succeeds on the physical device. The updated APK is currently running on the phone awaiting manual sign-in by the user.

---

## 2. Keystore & Signature Verification

Extracted directly via `.\gradlew.bat signingReport` and verified against the installed APK on the physical phone via `apksigner`:

```
Variant: debug
Config: debug
Store: C:\Users\gugan\.android\debug.keystore
Alias: AndroidDebugKey
MD5: E2:38:C8:55:F7:D0:A8:75:AA:DE:29:BF:94:46:5F:63
SHA1: 3D:06:F1:40:A2:88:91:8D:64:11:68:3A:1B:62:14:6F:A2:0E:9D:F1
SHA-256: BD:CA:77:98:2B:BA:B5:31:B5:36:BC:3A:FE:F3:A4:B8:E7:9C:6E:7E:DE:B5:C0:88:3F:7B:13:35:B7:F7:47:87
Valid until: Friday, September 29, 2056
```

### Signature Comparison Matrix

| Property | Configured / Expected Value | Actual Device-Installed Value | Match Status |
| :--- | :--- | :--- | :--- |
| **Package Name** | `com.stillfeed.app` | `com.stillfeed.app` | **EXACT MATCH** |
| **Signing Keystore** | `C:\Users\gugan\.android\debug.keystore` | `C:\Users\gugan\.android\debug.keystore` | **EXACT MATCH** |
| **Debug SHA-1** | `3D:06:F1:40:A2:88:91:8D:64:11:68:3A:1B:62:14:6F:A2:0E:9D:F1` | `3D:06:F1:40:A2:88:91:8D:64:11:68:3A:1B:62:14:6F:A2:0E:9D:F1` | **EXACT MATCH** |
| **Debug SHA-256** | `BD:CA:77:98:2B:BA:B5:31:B5:36:BC:3A:FE:F3:A4:B8:E7:9C:6E:7E:DE:B5:C0:88:3F:7B:13:35:B7:F7:47:87` | `BD:CA:77:98:2B:BA:B5:31:B5:36:BC:3A:FE:F3:A4:B8:E7:9C:6E:7E:DE:B5:C0:88:3F:7B:13:35:B7:F7:47:87` | **EXACT MATCH** |

The APK installed on the phone carries the exact package name `com.stillfeed.app` and SHA-1 `3D:06:F1:40:A2:88:91:8D:64:11:68:3A:1B:62:14:6F:A2:0E:9D:F1`.

---

## 3. Web Client ID, Credential Manager & Nonce Integration Analysis

### 1. Web OAuth Client ID
- **Verified Client ID:** `92258811699-dj7fh1fc8lh460h1rtm1f8k0f20pdk4m.apps.googleusercontent.com`
- **Replaced ID:** `92258811699-epmm3r02uj5ge76fc4ka07i59kpcqvvg.apps.googleusercontent.com`
- **Implementation:** Configured in `GoogleAuthConfig.DEFAULT_SERVER_CLIENT_ID` and referenced in `GoogleSignInHelper.kt`.
- **Architectural Requirement:** Credential Manager's `GetGoogleIdOption.Builder().setServerClientId(clientId)` consumes `GoogleAuthConfig.serverClientId`. This ensures Google Identity servers issue an ID token whose audience (`aud`) matches the Web Client ID expected by the backend.

### 2. Nonce Handling
- In `GoogleSignInHelper.kt`, `getGoogleIdToken(nonce: String? = null)` accepts an optional nonce.
- In Supabase Auth, when an ID token contains no nonce and no nonce is sent in the body of `auth/v1/token?grant_type=id_token`, token exchange verifies cleanly.
- If nonce verification is enforced in the Supabase Dashboard, the client generates a cryptographically secure raw nonce, passes the SHA-256 hash to `GetGoogleIdOption.Builder().setNonce(sha256Hex)`, and provides the raw nonce to `SupabaseHttpClient.authSignInWithIdToken(provider = "google", idToken, nonce = rawNonce)`.

### 3. Exception Diagnostics & Security
- When Google Play Services fails to issue an ID token, Credential Manager throws `GetCredentialException` (`GetCredentialCustomException`).
- Detailed exception diagnostics are logged to Logcat for debugging while user-friendly alerts are surfaced on screen.
- **Security Invariant:** Zero OAuth Client Secrets are stored in Android code, assets, or Git. Only public client IDs are utilized.

---

## 4. Google Play Services & Device Compatibility

Verified live on the physical device via ADB shell:

```
ADB Device ID:                ZA222WTN4H
Manufacturer / Model:         motorola edge 60 fusion (scout_g_sys)
Android Version:              Android 16 (API Level 36)
Google Play Services Package: com.google.android.gms
Google Play Services Version: 26.36.66 (260400-991556904)
Active Credential Provider:   com.google.android.gms/.auth.api.credentials.credman.service.GoogleIdService
Credential UI Component:      com.google.android.gms/.auth.api.credentials.assistedsignin.ui.AssistedSignInActivity
```

**Compatibility Result:** Fully supported. Android 16 natively integrates Credential Manager, and Google Play Services 26.36.66 actively services the request.

---

## 5. Live Physical Device Test Sequence & Error [28444] Resolution

### Previous Test & Error [28444] Encountered:
1. Previously, when tapping **Continue with Google** with mismatched Web Client ID `92258811699-epmm3r02uj5ge76fc4ka07i59kpcqvvg.apps.googleusercontent.com`, Google Play Services showed the account picker and consent dialog, but returned:
   `Google Sign-In error: [28444] Developer console is not set up correctly.`
2. The user cross-referenced the Google Cloud Console credentials dashboard and confirmed the correct active Web OAuth Client ID:
   `92258811699-dj7fh1fc8lh460h1rtm1f8k0f20pdk4m.apps.googleusercontent.com`.

### Final Remediation Applied:
1. Updated `GoogleAuthConfig.DEFAULT_SERVER_CLIENT_ID` in `GoogleSignInHelper.kt` to `92258811699-dj7fh1fc8lh460h1rtm1f8k0f20pdk4m.apps.googleusercontent.com`.
2. Updated unit test assertion in `GoogleSignInFlowTest.kt`.
3. Verified Credential Manager invokes `setServerClientId()` with this ID.
4. Preserved existing Android SHA-1 (`3D:06:F1:40:A2:88:91:8D:64:11:68:3A:1B:62:14:6F:A2:0E:9D:F1`) and package name (`com.stillfeed.app`).
5. Preserved Audience with test accounts in Google Cloud Console.
6. Preserved 6-digit email OTP, Resend SMTP settings, and Supabase email templates.
7. Preserved Figma-approved login UI without modification.

---

## 6. Automated Quality Gate Results

| Quality Gate | Requirement | Execution Result | Status |
| :--- | :--- | :--- | :--- |
| `.\gradlew.bat testDebugUnitTest` | Full unit test suite | **280 passed, 0 failed, 0 skipped** | **PASS** |
| `.\gradlew.bat assembleDebug` | Debug APK compilation | **BUILD SUCCESSFUL in 41s** | **PASS** |
| `.\gradlew.bat lintDebug` | Static code analysis | **BUILD SUCCESSFUL in 1m 47s (0 errors)** | **PASS** |
| Physical Device Install | Non-destructive reinstall (`-r`) | **Streamed Install Success (Data Preserved)** | **PASS** |
| Email OTP Verification | Real 6-digit numeric OTP | **PASS** (Confirmed working) | **PASS** |
| Post-Login Settings Nav | Stay on Settings without snapping | **PASS** (Race condition resolved; launchSingleTop added) | **PASS** |
| Session & Profile Persistence | Launch restoration & Room safety | **PASS** (App CoroutineScope hooked; getUser guarded) | **PASS** |
| Google Sign-In Flow | Real physical device sign-in | **AWAITING USER MANUAL TEST** (APK installed on phone) | **AWAITING USER TEST** |

---

## 7. Summary & Next Step Boundary

- **Email OTP:** **PASS** (100% operational).
- **Settings Navigation:** **PASS** (Race condition resolved between `AuthGate` transition splash and `Screen.Home.route`).
- **Session Restoration & Profile Safety:** **PASS** (Cold start session restore enabled via application scope; profile preservation active).
- **Google Sign-In:** **AWAITING USER MANUAL TEST** (Updated Web Client ID deployed to Motorola Edge 60 Fusion `ZA222WTN4H`, awaiting physical sign-in verification by user. Strictly NOT claimed as PASS until real login succeeds).
- **Approved UI & Resend Config:** 100% preserved. Zero regressions. Zero OAuth secrets stored or exposed.

**STOP before Step 16.**
