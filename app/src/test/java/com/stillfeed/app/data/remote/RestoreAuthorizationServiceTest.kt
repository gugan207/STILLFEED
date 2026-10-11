package com.stillfeed.app.data.remote

import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Dedicated test suite verifying the server-side security boundary of [RestoreAuthorizationService]
 * and the device enrollment anti-replacement rule (STEP 05 Remediation 3).
 *
 * Requirements Verified:
 * - Test A — First install: No existing binding -> ENROLLED.
 * - Test B — Second enrollment attempt: Existing active binding -> DENIED_ALREADY_BOUND.
 * - Test C — New device with valid account credentials: Calls enrollment -> DENIED_ALREADY_BOUND (existing binding/credential untouched).
 * - Test D — Same-device reinstall with recovered credential: Existing binding + valid credential -> AUTHORIZED (history restored).
 * - Test E — Same-device reinstall without recoverable credential -> DENIED (no new binding created).
 * - Test F — New device attempts restore: No valid original credential -> DENIED.
 * - Test G — New device attempts enrollment using arbitrary binding ID -> DENIED_ALREADY_BOUND.
 * - Test H — New device attempts enrollment using original binding ID -> DENIED_ALREADY_BOUND (original credential unchanged).
 * - Test I — Existing device history isolation: Returns only authenticated user + authorized binding history.
 * - Credential Rotation: Normal sign-in and session refresh do NOT rotate restore credential.
 * - Additional Boundaries: Unauthenticated caller, expired token, client parameter manipulation -> DENIED.
 */
class RestoreAuthorizationServiceTest {

    private lateinit var remoteAuthDataSource: InMemoryRemoteAuthDataSource
    private lateinit var remoteUserRepository: InMemoryRemoteUserRepository
    private lateinit var remoteHistoryRepository: InMemoryRemoteHistoryRepository
    private lateinit var restoreAuthService: InMemoryRestoreAuthorizationService

    private val userAlphaId = "user_alpha_uuid"
    private val validAlphaToken = "jwt_valid_alpha_signature"
    private val deviceBindingAlpha = "device_binding_phone_alpha"
    private val validAlphaRestoreCredential = "sfrc_alpha_secret_credential_1234567890_abcdef"

    private val otherBindingBeta = "device_binding_phone_beta"

    @Before
    fun setUp() = runBlocking {
        remoteAuthDataSource = InMemoryRemoteAuthDataSource()
        remoteUserRepository = InMemoryRemoteUserRepository()
        remoteHistoryRepository = InMemoryRemoteHistoryRepository()
        restoreAuthService = InMemoryRestoreAuthorizationService(
            remoteUserRepository = remoteUserRepository,
            remoteHistoryRepository = remoteHistoryRepository
        )

        // Seed valid session mapping on server (simulating JWT auth.uid() verification)
        restoreAuthService.registerValidToken(validAlphaToken, userAlphaId)

        // Seed trusted server enrollment for Device A:
        // Server stores hash of validAlphaRestoreCredential linked to deviceBindingAlpha
        restoreAuthService.seedEnrollment(
            userId = userAlphaId,
            deviceBindingId = deviceBindingAlpha,
            rawRestoreCredential = validAlphaRestoreCredential,
            isActive = true
        )
        remoteUserRepository.registerDeviceBinding(userAlphaId, deviceBindingAlpha)

        // Register user account in auth data source
        remoteAuthDataSource.registerExistingAccount(
            email = "alpha@stillfeed.app",
            password = "SecurePassword123!",
            userId = userAlphaId,
            displayName = "User Alpha",
            deviceBindingId = deviceBindingAlpha
        )

        // Seed history for Device A
        val usageAlpha = listOf(
            UsageEvent(
                id = "alpha_usage_1",
                timestampMillis = 1000L,
                packageName = "com.instagram.android",
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.BLOCKED,
                durationMillis = 30000L
            ),
            UsageEvent(
                id = "alpha_usage_2",
                timestampMillis = 2000L,
                packageName = "com.google.android.youtube",
                surfaceCategory = SurfaceCategory.SHORTS,
                eventType = UsageEventType.BLOCKED,
                durationMillis = 45000L
            )
        )
        val unlockAlpha = listOf(
            UnlockEvent(
                id = "alpha_unlock_1",
                timestampMillis = 1000L,
                allowanceMinutes = 15,
                reason = "Alpha break"
            )
        )
        remoteHistoryRepository.seedUsageHistory(userAlphaId, usageAlpha, deviceBindingAlpha)
        remoteHistoryRepository.seedUnlockHistory(userAlphaId, unlockAlpha, deviceBindingAlpha)

        // Seed separate history under a different binding (otherBindingBeta) for the same user
        val usageBeta = listOf(
            UsageEvent(
                id = "beta_usage_foreign",
                timestampMillis = 3000L,
                packageName = "com.zhiliaoapp.musically",
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.BLOCKED,
                durationMillis = 60000L
            )
        )
        remoteHistoryRepository.seedUsageHistory(userAlphaId, usageBeta, otherBindingBeta)
    }

    /**
     * Requirement: Test A — First install
     * Account has NO active device binding.
     * Expected: ENROLLED with server-issued restore credential.
     */
    @Test
    fun testA_firstInstall_noExistingBinding_enrolled() = runTest {
        val newUserId = "user_first_install_001"
        val newToken = "jwt_first_install_token"
        val newBindingId = "device_binding_first_phone"
        restoreAuthService.registerValidToken(newToken, newUserId)

        val enrollResult = restoreAuthService.enrollDeviceBinding(
            accessToken = newToken,
            deviceBindingId = newBindingId
        )

        assertTrue("First install enrollment must succeed", enrollResult.isSuccess)
        val enrollment = enrollResult.getOrThrow()
        assertTrue("Must return Enrolled result", enrollment is DeviceEnrollmentResult.Enrolled)
        val enrolled = enrollment as DeviceEnrollmentResult.Enrolled
        assertEquals(newBindingId, enrolled.deviceBindingId)
        assertTrue(enrolled.restoreCredential.startsWith("sfrc_"))
        assertTrue(enrolled.restoreCredential.length >= 32)

        // Confirm the newly issued credential can immediately authorize restore
        val restoreResult = restoreAuthService.requestRestoreAuthorization(
            accessToken = newToken,
            restoreCredential = enrolled.restoreCredential
        )
        assertTrue("Newly enrolled credential must be authorized", restoreResult is RestoreAuthorizationResult.Authorized)
    }

    /**
     * Requirement: Test B — Second enrollment attempt
     * Existing active binding already exists for this account.
     * Expected: DENIED_ALREADY_BOUND
     */
    @Test
    fun testB_secondEnrollmentAttempt_existingActiveBinding_deniedAlreadyBound() = runTest {
        val enrollResult = restoreAuthService.enrollDeviceBinding(
            accessToken = validAlphaToken,
            deviceBindingId = "device_binding_attempt_2"
        )

        assertTrue(enrollResult.isSuccess)
        val result = enrollResult.getOrThrow()
        assertTrue("Second enrollment must be DENIED_ALREADY_BOUND",
            result is DeviceEnrollmentResult.DeniedAlreadyBound)

        // Verify active binding remains untouched
        val activeEnrollment = restoreAuthService.getActiveEnrollment(userAlphaId)
        assertNotNull(activeEnrollment)
        assertEquals(deviceBindingAlpha, activeEnrollment?.deviceBindingId)
    }

    /**
     * Requirement: Test C — New device with valid account credentials
     * New device has valid account login credentials and attempts to call enrollment.
     * Expected: DENIED_ALREADY_BOUND. Existing binding and credential must remain unchanged.
     */
    @Test
    fun testC_newDeviceWithValidAccountCredentials_attemptsEnrollment_deniedAlreadyBound() = runTest {
        val newDeviceBindingId = "device_new_phone_gamma_123"
        val originalHashBefore = restoreAuthService.getActiveEnrollment(userAlphaId)?.restoreCredentialHash

        val enrollResult = restoreAuthService.enrollDeviceBinding(
            accessToken = validAlphaToken,
            deviceBindingId = newDeviceBindingId
        )

        assertTrue(enrollResult.isSuccess)
        val result = enrollResult.getOrThrow()
        assertTrue("New device enrollment on already bound account must be DENIED_ALREADY_BOUND",
            result is DeviceEnrollmentResult.DeniedAlreadyBound)

        // Existing binding and credential MUST remain unchanged
        val currentEnrollment = restoreAuthService.getActiveEnrollment(userAlphaId)
        assertEquals(deviceBindingAlpha, currentEnrollment?.deviceBindingId)
        assertEquals(originalHashBefore, currentEnrollment?.restoreCredentialHash)

        // Original credential remains fully operational
        val restoreResult = restoreAuthService.requestRestoreAuthorization(
            accessToken = validAlphaToken,
            restoreCredential = validAlphaRestoreCredential
        )
        assertTrue(restoreResult is RestoreAuthorizationResult.Authorized)
    }

    /**
     * Requirement: Test D — Same-device reinstall with recovered credential
     * Existing binding + valid recovered restore credential.
     * Expected: AUTHORIZED. Existing history is restored.
     */
    @Test
    fun testD_sameDeviceReinstallWithRecoveredCredential_authorized() = runTest {
        // App recovers validAlphaRestoreCredential from durable local storage
        val restoreResult = restoreAuthService.requestRestoreAuthorization(
            accessToken = validAlphaToken,
            restoreCredential = validAlphaRestoreCredential
        )

        assertTrue("Same-device reinstall with recovered credential must be AUTHORIZED",
            restoreResult is RestoreAuthorizationResult.Authorized)
        val authorized = restoreResult as RestoreAuthorizationResult.Authorized
        assertEquals(deviceBindingAlpha, authorized.authorizedBindingId)
        assertEquals(2, authorized.usageEvents.size)
        assertEquals(1, authorized.unlockEvents.size)
    }

    /**
     * Requirement: Test E — Same-device reinstall without recoverable credential
     * Durable credential cannot be recovered.
     * Expected: DENIED. No new binding is created.
     */
    @Test
    fun testE_sameDeviceReinstallWithoutRecoverableCredential_denied() = runTest {
        // 1. Attempting restore with blank or missing credential is denied
        val restoreResult = restoreAuthService.requestRestoreAuthorization(
            accessToken = validAlphaToken,
            restoreCredential = ""
        )
        assertTrue("Restore without recoverable credential must be DENIED",
            restoreResult is RestoreAuthorizationResult.DeniedUnauthorized)

        // 2. Client must NOT silently create a new binding
        val enrollAttempt = restoreAuthService.enrollDeviceBinding(
            accessToken = validAlphaToken,
            deviceBindingId = deviceBindingAlpha
        )
        assertTrue(enrollAttempt.isSuccess)
        val enrollResult = enrollAttempt.getOrThrow()
        assertTrue("Re-enrollment must be DENIED_ALREADY_BOUND; no new binding is created",
            enrollResult is DeviceEnrollmentResult.DeniedAlreadyBound)

        // Existing binding remains strictly bound to original credential
        val enrollment = restoreAuthService.getActiveEnrollment(userAlphaId)
        assertEquals(deviceBindingAlpha, enrollment?.deviceBindingId)
    }

    /**
     * Requirement: Test F — New device attempts restore
     * No valid original restore credential.
     * Expected: DENIED
     */
    @Test
    fun testF_newDeviceAttemptsRestore_denied() = runTest {
        // New device submits its own hardware ID or arbitrary string as restore credential
        val result = restoreAuthService.requestRestoreAuthorization(
            accessToken = validAlphaToken,
            restoreCredential = "device_gamma_unauthorized_token_12345"
        )

        assertTrue("New device without valid restore credential must be DENIED",
            result is RestoreAuthorizationResult.DeniedUnauthorized)
        val denied = result as RestoreAuthorizationResult.DeniedUnauthorized
        assertTrue(denied.reason.contains("Restore denied"))
    }

    /**
     * Requirement: Test G — New device attempts enrollment using arbitrary binding ID
     * Expected: DENIED_ALREADY_BOUND
     */
    @Test
    fun testG_newDeviceAttemptsEnrollmentUsingArbitraryBindingId_deniedAlreadyBound() = runTest {
        val result = restoreAuthService.enrollDeviceBinding(
            accessToken = validAlphaToken,
            deviceBindingId = "arbitrary_attacker_device_id_999999"
        )

        assertTrue(result.isSuccess)
        val enrollment = result.getOrThrow()
        assertTrue("Enrollment with arbitrary binding ID must be DENIED_ALREADY_BOUND",
            enrollment is DeviceEnrollmentResult.DeniedAlreadyBound)
        assertEquals(deviceBindingAlpha, restoreAuthService.getActiveEnrollment(userAlphaId)?.deviceBindingId)
    }

    /**
     * Requirement: Test H — New device attempts enrollment using the original binding ID
     * The attacker knows the original binding ID and attempts to re-enroll with it.
     * Expected: DENIED_ALREADY_BOUND.
     * The attacker must not be able to replace the original restore credential simply by knowing the binding ID.
     */
    @Test
    fun testH_newDeviceAttemptsEnrollmentUsingOriginalBindingId_deniedAlreadyBound() = runTest {
        val originalHash = restoreAuthService.getActiveEnrollment(userAlphaId)?.restoreCredentialHash

        val result = restoreAuthService.enrollDeviceBinding(
            accessToken = validAlphaToken,
            deviceBindingId = deviceBindingAlpha // Attacker supplies the original binding ID!
        )

        assertTrue(result.isSuccess)
        val enrollment = result.getOrThrow()
        assertTrue("Enrollment using known original binding ID must be DENIED_ALREADY_BOUND",
            enrollment is DeviceEnrollmentResult.DeniedAlreadyBound)

        // Original credential hash is unchanged
        val currentHash = restoreAuthService.getActiveEnrollment(userAlphaId)?.restoreCredentialHash
        assertEquals("Original credential hash must NOT be replaced", originalHash, currentHash)

        // Original credential still works
        val restoreResult = restoreAuthService.requestRestoreAuthorization(
            accessToken = validAlphaToken,
            restoreCredential = validAlphaRestoreCredential
        )
        assertTrue(restoreResult is RestoreAuthorizationResult.Authorized)
    }

    /**
     * Requirement: Test I — Existing device history isolation
     * Authorized restore returns only:
     *   user_id = authenticated user
     *   device_binding_id = server-authorized binding
     * No other binding's history may be returned.
     */
    @Test
    fun testI_existingDeviceHistoryIsolation_returnsOnlyAuthorizedBindingHistory() = runTest {
        val result = restoreAuthService.requestRestoreAuthorization(
            accessToken = validAlphaToken,
            restoreCredential = validAlphaRestoreCredential
        )

        assertTrue(result is RestoreAuthorizationResult.Authorized)
        val authorized = result as RestoreAuthorizationResult.Authorized

        // Binding isolation
        assertEquals(deviceBindingAlpha, authorized.authorizedBindingId)
        assertEquals(2, authorized.usageEvents.size)
        assertEquals(1, authorized.unlockEvents.size)

        // Verify foreign binding records (e.g. otherBindingBeta) are NOT returned
        val containsForeignRecords = authorized.usageEvents.any { it.id == "beta_usage_foreign" }
        assertTrue("History belonging to another binding must NEVER be returned", !containsForeignRecords)
    }

    /**
     * Requirement: SECTION 6 — VERIFY CREDENTIAL ROTATION (Normal Sign-In)
     * Calling normal account login/sign-in does NOT rotate the restore credential.
     */
    @Test
    fun verifyCredentialRotation_accountSignIn_doesNotRotateCredential() = runTest {
        val originalHash = restoreAuthService.getActiveEnrollment(userAlphaId)?.restoreCredentialHash
        assertNotNull(originalHash)

        // User signs in normally
        val signInResult = remoteAuthDataSource.signIn("alpha@stillfeed.app", "SecurePassword123!")
        assertTrue(signInResult.isSuccess)

        // Server-stored credential hash is identical
        val hashAfterSignIn = restoreAuthService.getActiveEnrollment(userAlphaId)?.restoreCredentialHash
        assertEquals("Account sign-in must NOT rotate the restore credential", originalHash, hashAfterSignIn)

        // Original restore credential remains valid
        val restoreResult = restoreAuthService.requestRestoreAuthorization(
            accessToken = validAlphaToken,
            restoreCredential = validAlphaRestoreCredential
        )
        assertTrue(restoreResult is RestoreAuthorizationResult.Authorized)
    }

    /**
     * Requirement: SECTION 6 — VERIFY CREDENTIAL ROTATION (Session Refresh)
     * Normal session refresh does NOT rotate the restore credential.
     */
    @Test
    fun verifyCredentialRotation_sessionRefresh_doesNotRotateCredential() = runTest {
        val originalHash = restoreAuthService.getActiveEnrollment(userAlphaId)?.restoreCredentialHash
        assertNotNull(originalHash)

        // User refreshes session
        val refreshResult = remoteAuthDataSource.refreshSession("refresh_valid_token_01")
        assertTrue(refreshResult.isSuccess)

        // Server-stored credential hash is identical
        val hashAfterRefresh = restoreAuthService.getActiveEnrollment(userAlphaId)?.restoreCredentialHash
        assertEquals("Session refresh must NOT rotate the restore credential", originalHash, hashAfterRefresh)

        // Original restore credential remains valid
        val restoreResult = restoreAuthService.requestRestoreAuthorization(
            accessToken = validAlphaToken,
            restoreCredential = validAlphaRestoreCredential
        )
        assertTrue(restoreResult is RestoreAuthorizationResult.Authorized)
    }

    /**
     * Security Boundary: Unauthenticated caller is denied
     */
    @Test
    fun securityBoundary_unauthenticatedCaller_isDenied() = runTest {
        val result = restoreAuthService.requestRestoreAuthorization(
            accessToken = "forged_unsigned_token",
            restoreCredential = validAlphaRestoreCredential
        )

        assertTrue(result is RestoreAuthorizationResult.ServerError)
        val serverError = result as RestoreAuthorizationResult.ServerError
        assertTrue(serverError.error is SecurityException)
    }

    /**
     * Security Boundary: Expired or revoked authorization is denied
     */
    @Test
    fun securityBoundary_expiredOrRevokedAuthorization_isDenied() = runTest {
        restoreAuthService.revokeToken(validAlphaToken)

        val result = restoreAuthService.requestRestoreAuthorization(
            accessToken = validAlphaToken,
            restoreCredential = validAlphaRestoreCredential
        )

        assertTrue("Revoked token must be DENIED", result is RestoreAuthorizationResult.Expired)
    }

    /**
     * Security Boundary: Client-controlled parameter manipulation cannot bypass authorization
     */
    @Test
    fun securityBoundary_clientControlledParameters_cannotBypassAuthorization() = runTest {
        val candidateAttacks = listOf(
            "x-device-binding-proof: $deviceBindingAlpha",
            "SELECT * FROM usage_history",
            "ANDROID_ID_aabbccddeeff1122",
            "device_model_pixel_8_pro",
            "null",
            "   ",
            "random_client_generated_uuid_${UUID.randomUUID()}"
        )

        for (candidate in candidateAttacks) {
            val result = restoreAuthService.requestRestoreAuthorization(
                accessToken = validAlphaToken,
                restoreCredential = candidate
            )
            assertTrue("Client parameter manipulation '$candidate' must be DENIED",
                result is RestoreAuthorizationResult.DeniedUnauthorized)
        }
    }
}
