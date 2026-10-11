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
