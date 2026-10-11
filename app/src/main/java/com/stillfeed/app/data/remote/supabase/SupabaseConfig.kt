package com.stillfeed.app.data.remote.supabase

/**
 * Verified configuration constants for the STILLFEED Supabase project.
 *
 * Project Reference: hrcazizzsbdmcnewowut
 * Project URL: https://hrcazizzsbdmcnewowut.supabase.co
 *
 * SECURITY INVARIANT:
 * Only public anon/publishable keys are included.
 * Service role keys, database passwords, and private secrets are STRICTLY FORBIDDEN
 * from client code and source control.
 */
object SupabaseConfig {
    const val PROJECT_ID: String = "hrcazizzsbdmcnewowut"
    const val PROJECT_URL: String = "https://hrcazizzsbdmcnewowut.supabase.co"

    /**
     * Legacy JWT anon key for standard PostgREST and Supabase Auth client operations.
     */
    const val ANON_KEY: String =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhyY2F6aXp6c2JkbWNuZXdvd3V0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE3MTczOTMsImV4cCI6MjEwNzI5MzM5M30.dtZb2XE1afh2m8h_NnZMrxLklUBsgbPnAOetU_M3AK4"

    /**
     * Modern publishable key format for project identification.
     */
    const val PUBLISHABLE_KEY: String = "sb_publishable_NKXB8DYl47W6t0QDDzxbKg_mcSethK7"

    /**
     * API Subpaths.
     */
    const val AUTH_PATH: String = "/auth/v1"
    const val REST_PATH: String = "/rest/v1"
}
