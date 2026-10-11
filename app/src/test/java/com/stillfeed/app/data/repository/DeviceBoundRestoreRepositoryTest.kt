package com.stillfeed.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.auth.InMemorySessionRepository
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.local.device.InMemoryDeviceBindingProvider
import com.stillfeed.app.data.local.device.InMemoryDeviceRestoreCredentialStore
import com.stillfeed.app.data.remote.InMemoryRemoteHistoryRepository
import com.stillfeed.app.data.remote.InMemoryRemoteUserRepository
import com.stillfeed.app.data.remote.InMemoryRestoreAuthorizationService
import com.stillfeed.app.domain.model.AuthSession
import com.stillfeed.app.domain.model.RestoreDecision
import com.stillfeed.app.domain.model.RestoreExecutionResult
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceBoundRestoreRepositoryTest {

    private lateinit var database: StillfeedDatabase
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var remoteUserRepository: InMemoryRemoteUserRepository
    private lateinit var remoteHistoryRepository: InMemoryRemoteHistoryRepository
    private lateinit var restoreAuthService: InMemoryRestoreAuthorizationService
    private lateinit var sessionRepository: InMemorySessionRepository
    private lateinit var restoreCredentialStore: InMemoryDeviceRestoreCredentialStore
    private lateinit var mockDeviceBindingProvider: InMemoryDeviceBindingProvider
    private lateinit var deviceBindingRepository: LocalDeviceBindingRepository
    private lateinit var usageRepository: LocalUsageRepository
    private lateinit var unlockHistoryRepository: LocalUnlockHistoryRepository

    private lateinit var restoreRepository: DefaultDeviceBoundRestoreRepository

    private val testUserId = "user_cloud_restore_01"
    private val testAccessToken = "jwt_access_valid_token_01"
    private val deviceTokenAlpha = "durable_platform_token_alpha"
    private val validAlphaRestoreCredential = "sfrc_alpha_secure_restore_credential_9876543210"
    private val deviceTokenBetaDifferentDevice = "durable_platform_token_beta"

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)

        remoteUserRepository = InMemoryRemoteUserRepository()
        remoteHistoryRepository = InMemoryRemoteHistoryRepository()
        restoreAuthService = InMemoryRestoreAuthorizationService(
            remoteUserRepository = remoteUserRepository,
            remoteHistoryRepository = remoteHistoryRepository
        )
        sessionRepository = InMemorySessionRepository()
        restoreCredentialStore = InMemoryDeviceRestoreCredentialStore()

        // Set up active authenticated session and register valid JWT on the server
        val activeSession = AuthSession(
            userId = testUserId,
            accessToken = testAccessToken,
            refreshToken = "refresh_valid_token_01",
            expiresAtMillis = System.currentTimeMillis() + 3600_000L,
            userEmail = "user@stillfeed.app",
            deviceBindingId = deviceTokenAlpha
        )
        sessionRepository.saveSession(activeSession)
        restoreAuthService.registerValidToken(testAccessToken, testUserId)

        // Seed server-side enrollment for Device A
        restoreAuthService.seedEnrollment(
            userId = testUserId,
            deviceBindingId = deviceTokenAlpha,
            rawRestoreCredential = validAlphaRestoreCredential,
            isActive = true
        )
        remoteUserRepository.registerDeviceBinding(testUserId, deviceTokenAlpha)

        // Save server-issued credential in local secure store (simulating same-device reinstall survival)
        restoreCredentialStore.saveRestoreCredential(validAlphaRestoreCredential)

        mockDeviceBindingProvider = InMemoryDeviceBindingProvider(
            currentToken = deviceTokenAlpha,
            isPlatformDurableAcrossReinstall = true
        )
        deviceBindingRepository = LocalDeviceBindingRepository(
            deviceBindingDao = database.deviceBindingDao(),
            deviceBindingProvider = mockDeviceBindingProvider,
            ioDispatcher = testDispatcher
        )
        usageRepository = LocalUsageRepository(
            usageEventDao = database.usageEventDao(),
            ioDispatcher = testDispatcher
        )
        unlockHistoryRepository = LocalUnlockHistoryRepository(
            unlockHistoryDao = database.unlockHistoryDao(),
            ioDispatcher = testDispatcher
        )

        restoreRepository = DefaultDeviceBoundRestoreRepository(
            remoteUserRepository = remoteUserRepository,
            restoreAuthorizationService = restoreAuthService,
            deviceBindingRepository = deviceBindingRepository,
            sessionRepository = sessionRepository,
            usageRepository = usageRepository,
            unlockHistoryRepository = unlockHistoryRepository,
            restoreCredentialStore = restoreCredentialStore,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Requirement: SAME DEVICE PRE-CHECK (Layer 1)
     * Local hardware signal matches registered token -> Layer 1 permits proceeding to server.
     */
    @Test
    fun evaluateRestoreEligibility_sameDeviceMatchingProof_permitsRestore() = runTest(testDispatcher) {
        val decision = restoreRepository.evaluateRestoreEligibility(testUserId)

        assertTrue("Decision must be Permitted for same device", decision is RestoreDecision.Permitted)
        val permitted = decision as RestoreDecision.Permitted
        assertEquals(deviceTokenAlpha, permitted.deviceBindingId)
    }

    /**
     * Requirement: DIFFERENT DEVICE PRE-CHECK (Layer 1)
     * Local hardware signal does not match registered token -> Layer 1 immediately rejects (UX gate).
     */
    @Test
    fun evaluateRestoreEligibility_differentDeviceProof_deniesRestore() = runTest(testDispatcher) {
        // Registered binding on server is beta (different device)
        remoteUserRepository.registerDeviceBinding(testUserId, deviceTokenBetaDifferentDevice)

        val decision = restoreRepository.evaluateRestoreEligibility(testUserId)

        assertTrue("Decision must be DeniedDifferentDevice for mismatched device", decision is RestoreDecision.DeniedDifferentDevice)
        val denied = decision as RestoreDecision.DeniedDifferentDevice
        assertEquals(deviceTokenBetaDifferentDevice, denied.registeredBindingId)
        assertEquals(deviceTokenAlpha, denied.currentBindingId)
    }

    /**
     * Requirement: INDETERMINATE DEVICE PRE-CHECK (Layer 1)
     */
    @Test
    fun evaluateRestoreEligibility_indeterminatePlatformSignal_deniesRestore() = runTest(testDispatcher) {
        mockDeviceBindingProvider.isPlatformDurableAcrossReinstall = false

        val decision = restoreRepository.evaluateRestoreEligibility(testUserId)

        assertTrue("Decision must be DeniedIndeterminateDevice for non-durable platform", decision is RestoreDecision.DeniedIndeterminateDevice)
    }

    /**
     * Requirement: NO REMOTE BACKUP RECORDED
     */
    @Test
    fun evaluateRestoreEligibility_noRemoteBackupRecorded_returnsNoRemoteBackupFound() = runTest(testDispatcher) {
        val decision = restoreRepository.evaluateRestoreEligibility("brand_new_user_without_backup")

        assertEquals(RestoreDecision.NoRemoteBackupFound, decision)
    }

    /**
     * Requirement: EXECUTE RESTORE - SAME DEVICE SUCCESS
     * Authenticated session + valid server-issued credential -> history restored into Room.
     */
    @Test
    fun executeDeviceBoundRestore_permittedSameDevice_restoresEventsIntoLocalRoom() = runTest(testDispatcher) {
        val usageEvents = listOf(
            UsageEvent(
                id = "usage_cloud_1",
                timestampMillis = 1000L,
                packageName = "com.instagram.android",
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.BLOCKED,
                durationMillis = 60000L
            ),
            UsageEvent(
                id = "usage_cloud_2",
                timestampMillis = 2000L,
                packageName = "com.google.android.youtube",
                surfaceCategory = SurfaceCategory.SHORTS,
                eventType = UsageEventType.BLOCKED,
                durationMillis = 120000L
            )
        )
        val unlockEvents = listOf(
            UnlockEvent(
                id = "unlock_cloud_1",
                timestampMillis = 1000L,
                allowanceMinutes = 15,
                reason = "Intentional check"
            )
        )
        remoteHistoryRepository.seedUsageHistory(testUserId, usageEvents, deviceTokenAlpha)
        remoteHistoryRepository.seedUnlockHistory(testUserId, unlockEvents, deviceTokenAlpha)

        assertEquals(0, usageRepository.getEventCount())

        val result = restoreRepository.executeDeviceBoundRestore(testUserId)

        assertTrue("Expected Success, got $result", result is RestoreExecutionResult.Success)
        val success = result as RestoreExecutionResult.Success
        assertEquals(2, success.restoredUsageEventsCount)
        assertEquals(1, success.restoredUnlockEventsCount)

        // Verify data actually merged into local Room tables
        assertEquals(2, usageRepository.getEventCount())
        assertNotNull(usageRepository.getEventById("usage_cloud_1"))
        assertNotNull(usageRepository.getEventById("usage_cloud_2"))
        assertNotNull(unlockHistoryRepository.getUnlockById("unlock_cloud_1"))
    }

    /**
     * Security Requirement: DEVICE WITHOUT SERVER-ISSUED CREDENTIAL
     * Even if Device B possesses valid account credentials and knows the binding ID,
     * missing the server-issued restore credential blocks restore.
     */
    @Test
    fun executeDeviceBoundRestore_missingServerIssuedCredential_blocksRestore() = runTest(testDispatcher) {
        // Clear restore credential from device storage (simulating Device B)
        restoreCredentialStore.clearRestoreCredential()

        val usageEvents = listOf(
            UsageEvent(
                id = "secret_event",
                timestampMillis = 1000L,
                packageName = "com.instagram.android",
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.BLOCKED,
                durationMillis = 60000L
            )
        )
        remoteHistoryRepository.seedUsageHistory(testUserId, usageEvents, deviceTokenAlpha)

        val result = restoreRepository.executeDeviceBoundRestore(testUserId)

        assertTrue("Missing server-issued credential must DENY restore", result is RestoreExecutionResult.Denied)
        assertEquals(0, usageRepository.getEventCount())
        assertNull(usageRepository.getEventById("secret_event"))
    }

    /**
     * Security Requirement: DEVICE WITH FORGED / WRONG CREDENTIAL
     * Device presenting an invalid credential string is rejected by server.
     */
    @Test
    fun executeDeviceBoundRestore_forgedCredential_rejectedByServer() = runTest(testDispatcher) {
        restoreCredentialStore.saveRestoreCredential("forged_invalid_credential_string_123456")

        val result = restoreRepository.executeDeviceBoundRestore(testUserId)

        assertTrue("Forged credential must be DENIED by server", result is RestoreExecutionResult.Denied)
        assertEquals(0, usageRepository.getEventCount())
    }

    /**
     * Requirement: EXECUTE RESTORE - CROSS-DEVICE REJECTION (Layer 1 Pre-Check)
     */
    @Test
    fun executeDeviceBoundRestore_differentDevicePreCheck_blocksRestoreAndLeavesRoomEmpty() = runTest(testDispatcher) {
        remoteUserRepository.registerDeviceBinding(testUserId, deviceTokenBetaDifferentDevice)

        val usageEvents = listOf(
            UsageEvent(
                id = "usage_cloud_secret",
                timestampMillis = 1000L,
                packageName = "com.instagram.android",
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.BLOCKED,
                durationMillis = 60000L
            )
        )
        remoteHistoryRepository.seedUsageHistory(testUserId, usageEvents, deviceTokenBetaDifferentDevice)

        val result = restoreRepository.executeDeviceBoundRestore(testUserId)

        assertTrue("Must be Denied on different device", result is RestoreExecutionResult.Denied)
        assertEquals(0, usageRepository.getEventCount())
        assertNull(usageRepository.getEventById("usage_cloud_secret"))
    }

    /**
     * Requirement: EXECUTE RESTORE - INDETERMINATE PLATFORM SIGNAL
     */
    @Test
    fun executeDeviceBoundRestore_indeterminatePlatform_blocksRestoreAndLeavesRoomEmpty() = runTest(testDispatcher) {
        mockDeviceBindingProvider.isPlatformDurableAcrossReinstall = false

        val result = restoreRepository.executeDeviceBoundRestore(testUserId)

        assertTrue("Must be Denied when platform signal is indeterminate", result is RestoreExecutionResult.Denied)
        assertEquals(0, usageRepository.getEventCount())
    }

    /**
     * Security Requirement: NO ACTIVE SESSION
     */
    @Test
    fun executeDeviceBoundRestore_noActiveSession_failsWithoutCallingServer() = runTest(testDispatcher) {
        sessionRepository.clearSession()

        val result = restoreRepository.executeDeviceBoundRestore(testUserId)

        assertTrue("Must be Failure when session is absent", result is RestoreExecutionResult.Failure)
        assertEquals(0, usageRepository.getEventCount())
    }

    /**
     * Security Requirement: UNAUTHENTICATED TOKEN REJECTED BY SERVER
     */
    @Test
    fun executeDeviceBoundRestore_unauthenticatedToken_serverRejects() = runTest(testDispatcher) {
        sessionRepository.saveSession(
            AuthSession(
                userId = testUserId,
                accessToken = "forged_unknown_jwt_token",
                refreshToken = "ref",
                expiresAtMillis = System.currentTimeMillis() + 3600_000L,
                userEmail = "user@stillfeed.app"
            )
        )

        val result = restoreRepository.executeDeviceBoundRestore(testUserId)

        assertTrue("Must be Failure when server cannot verify token", result is RestoreExecutionResult.Failure)
        val failure = result as RestoreExecutionResult.Failure
        assertTrue(failure.error is SecurityException)
        assertEquals(0, usageRepository.getEventCount())
    }

    /**
     * Security Requirement: EXPIRED OR REVOKED RESTORE TOKEN
     */
    @Test
    fun executeDeviceBoundRestore_revokedToken_fails() = runTest(testDispatcher) {
        restoreAuthService.revokeToken(testAccessToken)

        val result = restoreRepository.executeDeviceBoundRestore(testUserId)

        assertTrue("Must be Failure when token is revoked", result is RestoreExecutionResult.Failure)
        assertEquals(0, usageRepository.getEventCount())
    }

    /**
     * Security Requirement: SERVER INTERNAL ERROR
     */
    @Test
    fun executeDeviceBoundRestore_serverInternalError_returnsFailureResult() = runTest(testDispatcher) {
        restoreAuthService.simulateServerError = true

        val result = restoreRepository.executeDeviceBoundRestore(testUserId)

        assertTrue("Must report Failure on server error", result is RestoreExecutionResult.Failure)
        assertEquals(0, usageRepository.getEventCount())
    }
}
