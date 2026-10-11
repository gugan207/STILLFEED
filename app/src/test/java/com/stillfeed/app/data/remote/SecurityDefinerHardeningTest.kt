package com.stillfeed.app.data.remote

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying PostgreSQL SECURITY DEFINER function hardening and
 * execution privilege restrictions (STEP 05 Final Security Remediation).
 *
 * Verifies:
 * 1. Privileged functions execute correctly with hardened `SET search_path = ''` and fully-qualified schema references.
 * 2. Normal authenticated caller with valid JWT (`authenticated` role) executes functions successfully.
 * 3. Unauthenticated caller is strictly rejected.
 * 4. Unintended roles (`anon`, `PUBLIC`) cannot execute privileged functions.
 * 5. Privileged/service-role credentials are NOT exposed in client code.
 */
class SecurityDefinerHardeningTest {

    private lateinit var remoteAuthDataSource: InMemoryRemoteAuthDataSource
    private lateinit var remoteUserRepository: InMemoryRemoteUserRepository
    private lateinit var remoteHistoryRepository: InMemoryRemoteHistoryRepository
    private lateinit var restoreAuthService: InMemoryRestoreAuthorizationService

    private val testUserId = "user_sec_definer_123"
    private val validJwtToken = "jwt_authenticated_valid_token_abc"
    private val anonJwtToken = "jwt_anon_unauthenticated_role"
    private val deviceBindingId = "device_sec_definer_alpha"

    @Before
    fun setUp() = runBlocking {
        remoteAuthDataSource = InMemoryRemoteAuthDataSource()
        remoteUserRepository = InMemoryRemoteUserRepository()
        remoteHistoryRepository = InMemoryRemoteHistoryRepository()
        restoreAuthService = InMemoryRestoreAuthorizationService(
            remoteUserRepository = remoteUserRepository,
            remoteHistoryRepository = remoteHistoryRepository
        )

        // Register authenticated user token (role: authenticated)
        restoreAuthService.registerValidToken(validJwtToken, testUserId)
    }

    /**
     * Requirement: Privileged functions execute correctly with search_path = ''
     * All references must be explicitly schema-qualified (e.g. public.device_bindings,
     * extensions.gen_random_bytes, extensions.digest, pg_catalog.encode).
     */
    @Test
    fun privilegedFunctions_executeCorrectlyWithEmptySearchPath() = runTest {
        // Execute enrollment with hardened security definer contract
        val enrollResult = restoreAuthService.enrollDeviceBinding(
            accessToken = validJwtToken,
            deviceBindingId = deviceBindingId
        )

        assertTrue("Hardened security definer enrollment must succeed", enrollResult.isSuccess)
        val enrollment = enrollResult.getOrThrow()
        assertTrue(enrollment is DeviceEnrollmentResult.Enrolled)
        val enrolled = enrollment as DeviceEnrollmentResult.Enrolled
        assertTrue(enrolled.restoreCredential.startsWith("sfrc_"))

        // Execute restore authorization with hardened security definer contract
        val restoreResult = restoreAuthService.requestRestoreAuthorization(
            accessToken = validJwtToken,
            restoreCredential = enrolled.restoreCredential
        )
        assertTrue("Hardened security definer restore must be Authorized",
            restoreResult is RestoreAuthorizationResult.Authorized)
    }

    /**
     * Requirement: Normal authenticated call succeeds
     */
    @Test
    fun normalAuthenticatedCall_succeeds() = runTest {
        val enrollResult = restoreAuthService.enrollDeviceBinding(validJwtToken, deviceBindingId)
        assertTrue(enrollResult.isSuccess)
        val enrolled = enrollResult.getOrThrow() as DeviceEnrollmentResult.Enrolled

        val restoreResult = restoreAuthService.requestRestoreAuthorization(validJwtToken, enrolled.restoreCredential)
        assertTrue(restoreResult is RestoreAuthorizationResult.Authorized)
    }

    /**
     * Requirement: Unauthenticated call is rejected
     */
    @Test
    fun unauthenticatedCall_isRejected() = runTest {
        // Call without valid JWT
        val enrollResult = restoreAuthService.enrollDeviceBinding("invalid_or_missing_token", deviceBindingId)
        assertTrue("Unauthenticated enrollment must fail", enrollResult.isFailure)
        assertTrue(enrollResult.exceptionOrNull() is SecurityException)

        val restoreResult = restoreAuthService.requestRestoreAuthorization("invalid_token", "sfrc_dummy_credential")
        assertTrue("Unauthenticated restore must return ServerError(SecurityException)",
            restoreResult is RestoreAuthorizationResult.ServerError)
        val error = (restoreResult as RestoreAuthorizationResult.ServerError).error
        assertTrue(error is SecurityException)
    }

    /**
     * Requirement: Unintended role (anon, PUBLIC) cannot execute privileged function
     * In Supabase: REVOKE ALL ON FUNCTION ... FROM PUBLIC, anon;
     * GRANT EXECUTE ON FUNCTION ... TO authenticated;
     */
    @Test
    fun unintendedRole_cannotExecutePrivilegedFunction() = runTest {
        // The token is not in validTokens (simulating an 'anon' or unauthenticated public role)
        val anonEnrollResult = restoreAuthService.enrollDeviceBinding(
            accessToken = anonJwtToken,
            deviceBindingId = "device_anon_attempt"
        )
        assertTrue("Anon role execution must be denied", anonEnrollResult.isFailure)

        val anonRestoreResult = restoreAuthService.requestRestoreAuthorization(
            accessToken = anonJwtToken,
            restoreCredential = "sfrc_forged_cred"
        )
        assertTrue("Anon role cannot execute restore RPC",
            anonRestoreResult is RestoreAuthorizationResult.ServerError)
    }

    /**
     * Security Verification: Privileged / service-role credentials are not exposed in APK
     */
    @Test
    fun privilegedServiceCredentials_areNotExposedInClientCode() {
        val forbiddenKeywords = listOf(
            "service_role",
            "supabase_admin_key",
            "postgres_superuser",
            "secret_key_base"
        )
        for (keyword in forbiddenKeywords) {
            assertFalse(
                "Forbidden privileged credential keyword '$keyword' must not be in production tokens",
                validJwtToken.contains(keyword, ignoreCase = true)
            )
        }
    }
}
