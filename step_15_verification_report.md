# STILLFEED — STEP 15 GOOGLE SIGN-IN DIAGNOSTICS & VERIFICATION REPORT
**Physical Device Test Analysis, Google Cloud Console Error 28444 Root Cause & Remediation Guide**

- **Repository:** [https://github.com/gugan207/STILLFEED](https://github.com/gugan207/STILLFEED)
- **Supabase Project ID:** `hrcazizzsbdmcnewowut`
- **Connected Physical Device:** Motorola Edge 60 Fusion (`ZA222WTN4H`, Android 16 / SDK 36)
- **Status:** **FAIL** (Google Sign-In blocked by external Google Cloud Console configuration error 28444; Email OTP: PASS)
- **Timestamp:** 2026-10-13T01:58:00+05:30

---

## 1. Executive Summary & Verification Matrix

| Authentication Mechanism | Verification Result | Root Cause / Current State |
| :--- | :--- | :--- |
| **Email OTP (6-Digit Numeric)** | **PASS** | Successfully verified with real Resend SMTP (`smtp.resend.com:587`) and Supabase GoTrue backend (`mailer_autoconfirm: false`). |
| **Google Sign-In (Credential Manager)** | **FAIL** | Tapping "Continue with Google" displays account picker and consent dialog, but fails on account selection with `[28444] Developer console is not set up correctly`. |
| **Login UI & Ergonomics** | **PASS** | 100% Figma-approved design preserved intact. No UI regressions, zero status bar or navigation bar clipping. |
| **Non-Destructive Deployment** | **PASS** | Reinstalled via `adb install -r` without wiping user data or preferences. |

> [!CAUTION]
> **Honest Verification Boundary:** Google Sign-In is marked as **FAIL** until a real login successfully authenticates and issues a valid session on the physical device. The exact root cause is external to client code and resides in Google Cloud Console OAuth 2.0 Client ID registration.

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

The APK installed on the phone is 100% confirmed to carry the exact package name `com.stillfeed.app` and SHA-1 `3D:06:F1:40:A2:88:91:8D:64:11:68:3A:1B:62:14:6F:A2:0E:9D:F1`.

---

## 3. Web Client ID, Credential Manager & Nonce Integration Analysis

### 1. Web OAuth Client ID vs. Android Client ID
- **Configured Client ID:** `92258811699-epmm3r02uj5ge76fc4ka07i59kpcqvvg.apps.googleusercontent.com`
- **Verification:** Verified in `GoogleAuthConfig.DEFAULT_SERVER_CLIENT_ID` and `GoogleSignInHelper`.
- **Architectural Requirement:** Credential Manager's `GetGoogleIdOption.Builder().setServerClientId(...)` requires the **Web Application** OAuth Client ID so Google Identity servers issue an ID token whose `aud` (audience) claim matches the Supabase Google provider configuration. Android Client IDs must **never** be passed as `serverClientId`.

### 2. Nonce Handling
- In `GoogleSignInHelper.kt`, `getGoogleIdToken(nonce: String? = null)` accepts an optional nonce.
- In Supabase Auth, when an ID token contains no nonce and no nonce is sent in the body of `auth/v1/token?grant_type=id_token`, token exchange verifies cleanly.
- If nonce verification is enforced in the Supabase Dashboard, the client must generate a cryptographically secure raw nonce, pass the SHA-256 hash to `GetGoogleIdOption.Builder().setNonce(sha256Hex)`, and provide the raw nonce to `SupabaseHttpClient.authSignInWithIdToken(provider = "google", idToken, nonce = rawNonce)`.

### 3. Exception Diagnostics
- When Google Play Services fails to issue an ID token, Credential Manager throws `GetCredentialException` (`GetCredentialCustomException`).
- Added logging in `GoogleSignInHelper.kt` to capture the underlying exception stack trace in logcat while cleanly presenting actionable user feedback in the UI banner.

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

## 5. Live Physical Device Test Sequence & Error [28444] Evidence

### Sequence Executed on Connected Device:
1. Rebuilt application: `.\gradlew.bat assembleDebug` (BUILD SUCCESSFUL).
2. Installed non-destructively: `adb install -r app-debug.apk` (Performing Streamed Install -> Success).
3. Launched STILLFEED: `com.stillfeed.app/.MainActivity` focused on screen.
4. Tapped **Continue with Google** (coordinate `[610, 2008]`):
   - Credential Manager initiated `GetGoogleIdOperation`.
   - Android bottom sheet presented the account list:
     - `guganthangaraj7@gmail.com`
     - `gugangaming892@gmail.com`
     - `gugangaming6@gmail.com`
     - `guganthangaraj96@gmail.com`
5. Selected account `guganthangaraj7@gmail.com`:
   - Google presented consent dialog: *"Sign in to STILLFEED — By continuing, Google will share your name, email address and profile picture with STILLFEED."*
6. Tapped **Continue as Gugan** (coordinate `[610, 2513]`):
   - Google Play Services contacted Google backend to issue the ID token.
   - Google backend rejected the authorization request:
     `CredManProvService: GetCredentialResponse error returned from framework`
   - UI surfaced alert:
     `Google Sign-In error: [28444] Developer console is not set up correctly.`

---

## 6. Exact Google Cloud Console Fields to Change

Error `[28444]` is the internal Google Play Services code for `DEVELOPER_CONSOLE_NOT_SET_UP_CORRECTLY`. It occurs because Google's backend cannot match the calling app's package name and SHA-1 signature with an authorized Android client in the Google Cloud project owning Web Client ID `92258811699`.

To fix this error, update the following fields in the [Google Cloud Console](https://console.cloud.google.com/):

### Action 1: Create or Verify Android OAuth 2.0 Client ID
1. Open Google Cloud Console and select the project associated with Project Number **`92258811699`** (check project number in Project Settings).
2. Navigate to **APIs & Services** -> **Credentials**.
3. Under **OAuth 2.0 Client IDs**, verify if an **Android** client ID exists. If not, click **Create Credentials** -> **OAuth client ID**.
4. Configure the fields **precisely** as follows:
   - **Application type:** `Android`
   - **Name:** `STILLFEED Android Debug`
   - **Package name:** `com.stillfeed.app`
   - **SHA-1 certificate fingerprint:** `3D:06:F1:40:A2:88:91:8D:64:11:68:3A:1B:62:14:6F:A2:0E:9D:F1`
5. Click **Create**.

> [!IMPORTANT]
> Both the **Web application** client ID (`92258811699-epmm3r02uj5ge76fc4ka07i59kpcqvvg.apps.googleusercontent.com`) and the **Android** client ID MUST reside inside the **SAME Google Cloud Project** (`92258811699`).

### Action 2: Configure OAuth Consent Screen & Test Users
1. In Google Cloud Console, navigate to **APIs & Services** -> **OAuth consent screen**.
2. **User Type:** Ensure it is set to **External**.
3. **Publishing Status:**
   - If the status is **Testing**:
     - Scroll to **Test users**.
     - Click **Add Users**.
     - Add: `guganthangaraj7@gmail.com`, `gugangaming892@gmail.com`, and any other test Gmail accounts.
     - *If test users are not added while in Testing mode, Google blocks token issuance.*
   - If the app is set to **In production**, any Google account can sign in without being explicitly listed.

### Action 3: Allow Propagation Time
- After saving changes in Google Cloud Console, Google's backend servers take **5 to 15 minutes** to distribute the updated Android OAuth client bindings to Google Play Services.

---

## 7. Automated Quality Gate Results

| Quality Gate | Requirement | Execution Result | Status |
| :--- | :--- | :--- | :--- |
| `.\gradlew.bat testDebugUnitTest` | Full unit test suite | **275 passed, 0 failed, 0 skipped** | **PASS** |
| `.\gradlew.bat assembleDebug` | Debug APK compilation | **BUILD SUCCESSFUL** | **PASS** |
| `.\gradlew.bat lintDebug` | Static code analysis | **BUILD SUCCESSFUL (0 errors)** | **PASS** |
| Physical Device Install | Non-destructive reinstall (`-r`) | **Streamed Install Success (Data Preserved)** | **PASS** |
| Email OTP Verification | Real 6-digit numeric OTP | **PASS** (Confirmed working) | **PASS** |
| Google Sign-In Flow | Real physical device sign-in | **FAIL** (`[28444] Developer console is not set up correctly`) | **FAIL** |

---

## 8. Summary & Next Step Boundary

- **Email OTP:** **PASS** (100% operational).
- **Google Sign-In:** **FAIL** (Awaiting Android OAuth Client ID / Test Users configuration in Google Cloud Console project `92258811699`).
- **Approved UI & Resend Config:** 100% preserved. Zero regressions.

**STOP before Step 16.**
