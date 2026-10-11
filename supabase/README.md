# STILLFEED — Supabase Configuration & Schema Documentation

## Overview
STILLFEED utilizes Supabase for cloud-backed device-bound restore and telemetry synchronization.

- **Project URL:** `https://<YOUR_PROJECT_REF>.supabase.co`
- **Authentication:** Standard Supabase Auth (JWT)
- **Database Engine:** PostgreSQL 17 with Row Level Security (RLS)

## Security Architecture & Invariants

1. **Client Keys Only:**
   - The Android client only uses the public **Anon / Publishable Key** (`anon` role).
   - The **Service Role Key** (`service_role`), database password, and administrative secrets are **STRICTLY FORBIDDEN** from the Android client and version control.

2. **Row Level Security (RLS):**
   - RLS is enabled on all tables (`device_bindings`, `usage_history`, `unlock_history`, `awareness_monthly_state`).
   - Every read/write is strictly scoped to `auth.uid() = user_id`.

3. **Single Active Binding & Security Definer RPCs:**
   - Client applications cannot directly `SELECT` from `device_bindings`.
   - Device binding enrollment and restores are performed exclusively through server-side `SECURITY DEFINER` stored procedures (`enroll_device_binding` and `authorize_device_restore`) with hardened `SET search_path = ''`.
   - Execute permissions are revoked from `PUBLIC` and `anon`, and restricted to `authenticated`.

## Client Configuration Example

In `com.stillfeed.app.data.remote.supabase.SupabaseConfig`:

```kotlin
object SupabaseConfig {
    const val PROJECT_ID: String = "your-project-ref"
    const val PROJECT_URL: String = "https://your-project-ref.supabase.co"

    // Public anon JWT key (safe for client bundling)
    const val ANON_KEY: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

    // Modern publishable key format
    const val PUBLISHABLE_KEY: String = "sb_publishable_..."

    const val AUTH_PATH: String = "/auth/v1"
    const val REST_PATH: String = "/rest/v1"
}
```

## Migration Files
- `supabase/migrations/20261011000000_stillfeed_initial_schema.sql`: Contains the complete DDL, RLS policies, table constraints, and hardened PL/pgSQL RPC definitions.

## Email Delivery & Resend Integration (Server-Side)
STILLFEED delivers OTP verification codes and password recovery links using Resend without bundling any private credentials into the Android APK.

Two server-side options are supported:

### Option A: Resend Custom SMTP (Recommended & Zero Code)
1. In Supabase Dashboard -> **Authentication** -> **SMTP Settings**:
   - **Sender Email:** `noreply@yourdomain.com` (or your verified domain)
   - **Sender Name:** `STILLFEED`
   - **Host:** `smtp.resend.com`
   - **Port:** `465` (SSL) or `587` (TLS)
   - **User:** `resend`
   - **Password:** `<RESEND_API_KEY>` (from resend.com)
2. Save changes. Supabase Auth will now dispatch all confirmation OTPs and recovery links via Resend.

### Option B: Supabase Auth "Send Email Hook" (Edge Function)
A custom hook template is provided in `supabase/functions/resend-email-hook/index.ts`.
1. Deploy the function:
   ```bash
   supabase secrets set RESEND_API_KEY="re_..." RESEND_FROM="STILLFEED <noreply@yourdomain.com>"
   supabase functions deploy resend-email-hook --no-verify-jwt
   ```
2. In Supabase Dashboard -> **Authentication** -> **Hooks**:
   - Enable "Send Email Hook" and select the `resend-email-hook` function.

## Google Sign-In Provider Setup (Server-Side)
To enable Google Sign-In on the Android client:
1. In the Google Cloud Console, create an **OAuth 2.0 Client ID** for **Android** (with your package name `com.stillfeed.app` and SHA-1 fingerprint) and a **Web Client ID**.
2. In Supabase Dashboard -> **Authentication** -> **Providers** -> **Google**:
   - Enable Google.
   - Enter your Google **Client ID** (Web application client ID).
   - Enter your Google **Client Secret** (Web application client secret).
   - Add authorized redirect URI: `https://hrcazizzsbdmcnewowut.supabase.co/auth/v1/callback`.
3. Save changes. The Android app passes the user's Google ID token to Supabase Auth (`/auth/v1/token?grant_type=id_token`) to establish the authenticated session.
