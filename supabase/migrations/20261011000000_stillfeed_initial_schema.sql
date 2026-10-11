-- STILLFEED Supabase Database Schema & Security Definer Hardening
-- Migration: 20261011000000_stillfeed_initial_schema.sql
-- Project Reference: hrcazizzsbdmcnewowut

-- 1. Device Bindings Table with Single Active Binding Unique Constraint
CREATE TABLE IF NOT EXISTS public.device_bindings (
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
CREATE TABLE IF NOT EXISTS public.usage_history (
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
CREATE TABLE IF NOT EXISTS public.unlock_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_binding_id TEXT NOT NULL,
    timestamp_ms BIGINT NOT NULL,
    allowance_minutes INT NOT NULL,
    reason TEXT NOT NULL
);

-- 4. Monthly Awareness State Table
CREATE TABLE IF NOT EXISTS public.awareness_monthly_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    month_identifier TEXT NOT NULL,
    target_reduction_percentage INT NOT NULL,
    baseline_minutes INT NOT NULL,
    current_progress_minutes INT NOT NULL DEFAULT 0,
    target_usage_minutes INT NOT NULL DEFAULT 0,
    reduction_amount_minutes INT NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_user_month_identifier UNIQUE (user_id, month_identifier)
);

-- 5. Enable Row Level Security (RLS) on all tables
ALTER TABLE public.device_bindings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.usage_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.unlock_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.awareness_monthly_state ENABLE ROW LEVEL SECURITY;

-- 6. Row Level Security Policies
-- Usage History RLS
CREATE POLICY "Users can insert own usage history" ON public.usage_history
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can select own usage history" ON public.usage_history
    FOR SELECT TO authenticated USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own usage history" ON public.usage_history
    FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- Unlock History RLS
CREATE POLICY "Users can insert own unlock history" ON public.unlock_history
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can select own unlock history" ON public.unlock_history
    FOR SELECT TO authenticated USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own unlock history" ON public.unlock_history
    FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- Monthly Awareness RLS
CREATE POLICY "Users can manage own awareness state" ON public.awareness_monthly_state
    FOR ALL TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Device Bindings RLS: direct select denied (client reads occur exclusively via SECURITY DEFINER RPC)
CREATE POLICY "Deny direct client select on device_bindings" ON public.device_bindings
    FOR SELECT TO authenticated USING (false);

CREATE POLICY "Users can delete own device bindings" ON public.device_bindings
    FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- 7. Hardened Device Enrollment Function (SECURITY DEFINER with search_path = '')
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
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthenticated caller';
    END IF;

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

    v_restore_credential := 'sfrc_' || pg_catalog.encode(extensions.gen_random_bytes(32), 'hex');
    v_restore_credential_hash := pg_catalog.encode(extensions.digest(v_restore_credential, 'sha256'), 'hex');

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

-- 8. Hardened Authoritative Device Restore Function (SECURITY DEFINER with search_path = '')
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

-- 9. RPC Execution Security Privileges
REVOKE ALL ON FUNCTION public.enroll_device_binding(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.enroll_device_binding(TEXT) FROM anon;
GRANT EXECUTE ON FUNCTION public.enroll_device_binding(TEXT) TO authenticated;

REVOKE ALL ON FUNCTION public.authorize_device_restore(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.authorize_device_restore(TEXT) FROM anon;
GRANT EXECUTE ON FUNCTION public.authorize_device_restore(TEXT) TO authenticated;
