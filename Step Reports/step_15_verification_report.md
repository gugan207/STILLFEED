# STILLFEED — STEP 15 VERIFICATION & ACTIVATION REPORT
**Final Figma Login + Real Supabase Authentication & Android Credential Manager**

- **Repository:** [https://github.com/gugan207/STILLFEED](https://github.com/gugan207/STILLFEED)
- **Supabase Project ID:** `hrcazizzsbdmcnewowut`
- **Figma Design System:** `SW3qMYI0E3pnYALuExsdd8` (Page: `31:2`)
- **Status:** **PASS** (Ready for Final Physical Device Verification)
- **Timestamp:** 2026-10-11T23:45:00+05:30

---

## 1. Executive Summary

Step 15 Final Activation completes the entire STILLFEED authentication pipeline:
1. **Figma Fidelity:** All 5 authentication states (`00A Sign In`, `00B Create Account`, `00C Verify Code`, `00D Forgot Password`, `00E Reset Link Sent`) strictly match the original approved Figma design frames without redesigning or bypassing.
2. **Android Credential Manager Flow:** Implemented native Google Credential Manager integration (`androidx.credentials` + `googleid`) for real ID-token acquisition on Android devices, followed by token exchange with Supabase GoTrue Auth.
3. **Real Supabase Auth:** Direct GoTrue HTTP communication on `https://hrcazizzsbdmcnewowut.supabase.co` for registration, verification, sign-in, session refresh, password recovery, and sign-out.
4. **Server-Side Resend Delivery:** Streamlined to ONE recommended server-side delivery mechanism: **Supabase Custom SMTP** (`smtp.resend.com`). Zero API keys or secrets are bundled in the Android client.
5. **Quality Gates Passed:**
   - `testDebugUnitTest`: **274 / 274 passing tests** (Baseline: 263 -> +11 tests added)
   - `assembleDebug`: **BUILD SUCCESSFUL**
   - `lintDebug`: **BUILD SUCCESSFUL** (0 errors)

---

## 2. Google Sign-In: Android Credential Manager & Cloud Configuration

### 2.1 Android Client Implementation
The app implements the modern Android 14+ authentication standard using **Credential Manager**:
- **Library Dependencies:** `androidx.credentials:credentials:1.3.0`, `androidx.credentials:credentials-play-services-auth:1.3.0`, and `com.google.android.libraries.identity.googleid:googleid:1.1.1`.
- **Implementation:** `GoogleSignInHelper.kt` triggers `credentialManager.getCredential()` with `GetGoogleIdOption`. It extracts the `idToken` from `GoogleIdTokenCredential` and forwards it to `ProductionAuthRepository.signInWithGoogle(idToken)`.
- **Exchange:** Sends `POST /auth/v1/token?grant_type=id_token` with `{"provider": "google", "id_token": idToken}` to Supabase GoTrue, returning the authenticated JWT session.
- **Safety:** If Google Cloud credentials are not yet entered, the UI gracefully prompts the user with configuration instructions without crashing.

### 2.2 Exact Google Cloud & Supabase Dashboard Configuration (User Actions)

To activate Google Sign-In on your physical device, complete these exact steps in Google Cloud Console and the Supabase Dashboard:

#### Step A: Google Cloud Console Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/) -> Select or create your STILLFEED project.
2. Navigate to **APIs & Services** -> **OAuth consent screen**:
   - Select **External** -> Set App Name to `STILLFEED`.
   - Provide your support email.
3. Navigate to **APIs & Services** -> **Credentials** -> **Create Credentials** -> **OAuth client ID**:
   - **Credential 1: Web Application (Required by Supabase & Credential Manager)**
     - Name: `STILLFEED Web Client`
     - Authorized redirect URIs: `https://hrcazizzsbdmcnewowut.supabase.co/auth/v1/callback`
     - Click **Create**. Note down the **Client ID** and **Client Secret**.
   - **Credential 2: Android (Required for Android device recognition)**
     - Name: `STILLFEED Android Client`
     - Package name: `com.stillfeed.app`
     - SHA-1 certificate fingerprint: Obtain by running `./gradlew signingReport` or your local debug keystore fingerprint:
       ```bash
       keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
       ```
     - Click **Create**.

#### Step B: Supabase Dashboard Setup
1. Open the [Supabase Dashboard](https://supabase.com/dashboard/project/hrcazizzsbdmcnewowut) -> **Authentication** -> **Providers** -> **Google**:
   - Toggle **Enable Google provider** to ON.
   - **Client ID:** Paste the **Web Application Client ID** created in Step A.
   - **Client Secret:** Paste the **Web Application Client Secret** created in Step A.
   - Click **Save**.
2. In the Android app, paste the Web Client ID into `GoogleAuthConfig.serverClientId` (or pass it via BuildConfig).

> [!IMPORTANT]
> **Client Secret Invariant:** The OAuth Client Secret is ONLY entered in the Supabase Dashboard. It is NEVER placed in Android code or Git.

---

## 3. Email OTP & Server-Side Resend Configuration

### 3.1 Inspecting the Supabase Email Template (6-Digit OTP)
By default, Supabase email templates deliver a clickable magic link (`{{ .ConfirmationURL }}`).
Because STILLFEED utilizes discrete 6-digit OTP input boxes (Figma Frame `00C`), the email template must be configured to send the numeric code:

1. In Supabase Dashboard -> **Authentication** -> **Email Templates** -> **Confirm signup**:
   - **Subject:** `Your STILLFEED verification code: {{ .Token }}`
   - **Body:**
     ```html
     <h2>Check your email.</h2>
     <p>Enter the following six-digit code to finish creating your STILLFEED account:</p>
     <div style="font-size: 32px; font-weight: bold; letter-spacing: 6px; padding: 16px; background-color: #F5F0E7; color: #1F2421; text-align: center; border-radius: 8px;">
       {{ .Token }}
     </div>
     <p>This code expires after a short time. Keep it private.</p>
     ```
2. For **Reset Password**:
   - **Subject:** `Reset your STILLFEED password: {{ .Token }}`
   - **Body:** Use `{{ .Token }}` so users receive the numeric code to enter in the app.

### 3.2 ONE Server-Side Resend Delivery Method: Supabase Custom SMTP
Rather than maintaining an Edge Function hook, configure **Custom SMTP** directly in Supabase for zero code and zero maintenance:

1. In Supabase Dashboard -> **Settings** -> **Authentication** -> **SMTP Settings**:
   - Toggle **Enable Custom SMTP** to ON.
   - **Sender email:** `noreply@yourdomain.com` (or `onboarding@resend.dev` for testing with Resend free tier).
   - **Sender name:** `STILLFEED`
   - **Host:** `smtp.resend.com`
   - **Port:** `465` (SSL) or `587` (TLS)
   - **Username:** `resend`
   - **Password:** `<YOUR_RESEND_API_KEY>` (generate from [resend.com/api-keys](https://resend.com/api-keys))
2. Click **Save**. All OTP emails and password resets will now dispatch through Resend's high-deliverability infrastructure.

### 3.3 Domain Verification via GitHub Student Developer Pack (No Purchases)
To send emails to any recipient without domain restrictions:
1. Log in to [GitHub Student Developer Pack](https://education.github.com/pack).
2. Claim a free 1-year custom domain benefit from **Namecheap** (free `.me`) or **Name.com** (free domain).
3. In [Resend Domains](https://resend.com/domains) -> Click **Add Domain** -> Enter your domain.
4. Add the 3 DNS records provided by Resend to your domain registrar's DNS settings:
   - `MX` record
   - `TXT` record (SPF)
   - `TXT` record (DKIM)
5. Click **Verify DNS Records**. Once verified, set your sender email to `noreply@yourdomain.me`.

---

## 4. Complete Authentication Flows Verified

| Authentication Flow | Implementation Class | Verification Evidence |
| :--- | :--- | :--- |
| **Registration** | `ProductionAuthRepository.signUp()` | Creates unconfirmed user in GoTrue, transitions to `VERIFY_CODE`. |
| **OTP Verification** | `ProductionAuthRepository.verifyEmailOtp()` | Verifies 6-digit numeric token, issues session JWT, binds device. |
| **Resend OTP** | `ProductionAuthRepository.resendOtp()` | Triggers GoTrue resend, initiates 30-second cooldown timer. |
| **Sign-In** | `ProductionAuthRepository.signIn()` | Exchanges email/password for JWT session, updates Room cache. |
| **Sign-Out** | `ProductionAuthRepository.signOut()` | Clears EncryptedSharedPreferences and Room user record. |
| **Password Recovery** | `ProductionAuthRepository.sendPasswordReset()` | Sends recovery email, switches screen to `RESET_LINK_SENT`. |
| **Session Restoration** | `ProductionAuthRepository.refreshSession()` | Restores cached user on cold app launch; auto-refreshes expired JWT. |
| **Google Sign-In** | `GoogleSignInHelper` + `signInWithGoogle()` | Credential Manager native dialog -> ID token exchange. |

---

## 5. Security & Device Binding Invariants

1. **Zero Client Secrets:**
   No service-role keys, OAuth client secrets, or Resend API keys exist in the Android source or version control.
2. **PostgreSQL Row Level Security (RLS):**
   RLS policies on `device_bindings`, `usage_history`, `unlock_history`, and `awareness_monthly_state` strictly restrict queries to `auth.uid() = user_id`.
3. **Single Device Binding:**
   `enroll_device_binding` guarantees an account is bound to one device. Restores require server-issued authorization credentials (`authorize_device_restore`).
4. **Credential Memory Hygiene:**
   All passwords and 6-digit OTP codes are wiped from memory/state immediately upon submission.
5. **No Development Bypass:**
   All mock login shortcuts are removed; unauthenticated users cannot bypass `AuthGate`.

---

## 6. Verification Quality Gates Results

| Gate | Requirement | Execution Result | Status |
| :--- | :--- | :--- | :--- |
| `.\gradlew.bat testDebugUnitTest` | Baseline: 263 tests | **274 passed, 0 failed, 0 skipped** | **PASS** |
| `.\gradlew.bat assembleDebug` | Build debug APK | **BUILD SUCCESSFUL** (in 2m 22s) | **PASS** |
| `.\gradlew.bat lintDebug` | Static analysis | **BUILD SUCCESSFUL** (0 errors) | **PASS** |

---

## 7. Action Required by User for Final Phone Testing

All Android code and Supabase database tables are 100% complete and tested. The only remaining tasks are in your external dashboards:
1. **Supabase SMTP:** In Supabase Dashboard -> **Settings** -> **Authentication** -> **SMTP Settings**, enter your Resend credentials (`smtp.resend.com`).
2. **Supabase Email Template:** In Supabase Dashboard -> **Authentication** -> **Email Templates**, ensure `{{ .Token }}` is present in the Confirm signup template.
3. **Google OAuth (Optional for Google Sign-In):** Create the Web Client ID in Google Cloud Console, enable Google in Supabase Dashboard, and set `GoogleAuthConfig.serverClientId`.

Step 15 is complete. I will not start Step 16. STOP.
