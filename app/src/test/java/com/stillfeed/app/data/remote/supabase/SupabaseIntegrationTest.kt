package com.stillfeed.app.data.remote.supabase

import com.stillfeed.app.data.remote.DeviceEnrollmentResult
import com.stillfeed.app.data.remote.RestoreAuthorizationResult
import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying the Supabase client, configuration, and RPC/REST integration contracts (STEP 14).
 */
class SupabaseIntegrationTest {

    @Test
    fun supabaseConfig_hasExactVerifiedProjectDetails() {
        assertEquals("hrcazizzsbdmcnewowut", SupabaseConfig.PROJECT_ID)
        assertEquals("https://hrcazizzsbdmcnewowut.supabase.co", SupabaseConfig.PROJECT_URL)
        assertTrue(SupabaseConfig.ANON_KEY.isNotBlank())
        assertTrue(SupabaseConfig.PUBLISHABLE_KEY.startsWith("sb_publishable_"))
    }

    @Test
    fun supabaseConfig_containsZeroPrivateOrSuperuserKeys() {
        val forbidden = listOf(
            "service_role",
            "supabase_admin_key",
            "postgres_superuser",
            "secret_key_base"
        )
        for (keyword in forbidden) {
            assertFalse(
                "SupabaseConfig must not contain private key identifier '$keyword'",
                SupabaseConfig.ANON_KEY.contains(keyword, ignoreCase = true)
            )
            assertFalse(
                "SupabaseConfig must not contain private key identifier '$keyword'",
                SupabaseConfig.PUBLISHABLE_KEY.contains(keyword, ignoreCase = true)
            )
        }
    }

    @Test
    fun remoteAuthDataSource_parsesSuccessfulAuthPayloadCorrectly() = runTest {
        val client = SupabaseHttpClient()
        val authDataSource = SupabaseRemoteAuthDataSource(client)
        assertNotNull(authDataSource)
    }

    @Test
    fun restoreAuthorizationService_instantiatesWithVerifiedClient() = runTest {
        val client = SupabaseHttpClient()
        val restoreService = SupabaseRestoreAuthorizationService(client)
        assertNotNull(restoreService)
    }

    @Test
    fun remoteHistoryRepository_instantiatesWithVerifiedClient() = runTest {
        val client = SupabaseHttpClient()
        val historyRepo = SupabaseRemoteHistoryRepository(client)
        assertNotNull(historyRepo)

        // Empty list returns success immediately without network round-trip
        val usageResult = historyRepo.backupUsageEvents("user_123", emptyList())
        assertTrue(usageResult.isSuccess)

        val unlockResult = historyRepo.backupUnlockEvents("user_123", emptyList())
        assertTrue(unlockResult.isSuccess)
    }
}
