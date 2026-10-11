package com.stillfeed.app.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.auth.model.AuthError
import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.local.device.InMemoryDeviceBindingProvider
import com.stillfeed.app.data.remote.InMemoryRemoteAuthDataSource
import com.stillfeed.app.data.remote.InMemoryRemoteUserRepository
import com.stillfeed.app.data.repository.LocalDeviceBindingRepository
import com.stillfeed.app.data.repository.LocalUserRepository
import com.stillfeed.app.domain.model.AuthSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ProductionAuthRepositoryTest {

    private lateinit var database: StillfeedDatabase
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var remoteAuthDataSource: InMemoryRemoteAuthDataSource
    private lateinit var sessionRepository: InMemorySessionRepository
    private lateinit var remoteUserRepository: InMemoryRemoteUserRepository
    private lateinit var mockDeviceBindingProvider: InMemoryDeviceBindingProvider
    private lateinit var deviceBindingRepository: LocalDeviceBindingRepository
    private lateinit var localUserRepository: LocalUserRepository

    private lateinit var repository: ProductionAuthRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)

        remoteAuthDataSource = InMemoryRemoteAuthDataSource()
        sessionRepository = InMemorySessionRepository()
        remoteUserRepository = InMemoryRemoteUserRepository()
        mockDeviceBindingProvider = InMemoryDeviceBindingProvider(
            currentToken = "device_test_token_001",
            isPlatformDurableAcrossReinstall = true
        )
        deviceBindingRepository = LocalDeviceBindingRepository(
            deviceBindingDao = database.deviceBindingDao(),
            deviceBindingProvider = mockDeviceBindingProvider,
            ioDispatcher = testDispatcher
        )
        localUserRepository = LocalUserRepository(
            userDao = database.userDao(),
            ioDispatcher = testDispatcher
        )

        repository = ProductionAuthRepository(
            remoteAuthDataSource = remoteAuthDataSource,
            sessionRepository = sessionRepository,
            remoteUserRepository = remoteUserRepository,
            deviceBindingRepository = deviceBindingRepository,
            userRepository = localUserRepository,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun signUp_success_createsAccount_bindsDevice_andSavesSession() = runTest(testDispatcher) {
        val result = repository.signUp("user@example.com", "SecurePassword123!", "Alice")

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("user@example.com", user.email)
        assertEquals("Alice", user.displayName)
        assertEquals("device_test_token_001", user.deviceBindingId)

        // Verify active state
        val authState = repository.authState.value
        assertTrue(authState is AuthState.SignedIn)
        assertEquals(user.id, (authState as AuthState.SignedIn).user.id)

        // Verify session saved
        val session = sessionRepository.getSession()
        assertNotNull(session)
        assertEquals(user.id, session?.userId)
        assertTrue(session?.accessToken?.startsWith("jwt_access_") == true)
        assertEquals("device_test_token_001", session?.deviceBindingId)

        // Verify remote user repository binding
        val remoteBinding = remoteUserRepository.getRegisteredDeviceBindingId(user.id).getOrNull()
        assertEquals("device_test_token_001", remoteBinding)

        // Verify local Room persistence
        val localUser = localUserRepository.getUser(user.id)
        assertNotNull(localUser)
        assertEquals("user@example.com", localUser?.email)
    }

    @Test
    fun signUp_duplicateEmail_failsWithAccountAlreadyExists() = runTest(testDispatcher) {
        remoteAuthDataSource.simulateAccountAlreadyExists = true

        val result = repository.signUp("existing@example.com", "Password123!", "Bob")

        assertTrue(result.isFailure)
        val authState = repository.authState.value
        assertTrue(authState is AuthState.Error)
        assertTrue((authState as AuthState.Error).error is AuthError.AccountAlreadyExists)
        assertNull(sessionRepository.getSession())
    }

    @Test
    fun signUp_networkFailure_failsWithNetworkFailure() = runTest(testDispatcher) {
        remoteAuthDataSource.simulateNetworkError = true

        val result = repository.signUp("user@example.com", "Password123!", "Bob")

        assertTrue(result.isFailure)
        val authState = repository.authState.value
        assertTrue(authState is AuthState.Error)
        assertTrue((authState as AuthState.Error).error is AuthError.NetworkFailure)
    }

    @Test
    fun signUp_malformedResponse_failsGracefully() = runTest(testDispatcher) {
        remoteAuthDataSource.simulateMalformedResponse = true

        val result = repository.signUp("user@example.com", "Password123!", "Bob")

        assertTrue(result.isFailure)
        val authState = repository.authState.value
        assertTrue(authState is AuthState.Error)
    }

    @Test
    fun signIn_validCredentials_authenticates_andPersistsUserLocally() = runTest(testDispatcher) {
        remoteAuthDataSource.registerExistingAccount(
            email = "alice@example.com",
            password = "Password123!",
            userId = "alice_remote_id",
            displayName = "Alice",
            deviceBindingId = "registered_binding_999"
        )

        val result = repository.signIn("alice@example.com", "Password123!")

        assertTrue(result.isSuccess)
        val authUser = result.getOrThrow()
        assertEquals("alice@example.com", authUser.email)
        assertEquals("alice_remote_id", authUser.id)
        assertEquals("registered_binding_999", authUser.deviceBindingId)

        // Session
        val session = sessionRepository.getSession()
        assertNotNull(session)
        assertEquals("alice_remote_id", session?.userId)
        assertEquals("registered_binding_999", session?.deviceBindingId)

        // Local cache
        val local = localUserRepository.getUser("alice_remote_id")
        assertNotNull(local)
        assertEquals("alice@example.com", local?.email)

        assertTrue(repository.authState.value is AuthState.SignedIn)
    }

    @Test
    fun signIn_invalidPassword_failsWithInvalidCredentials() = runTest(testDispatcher) {
        remoteAuthDataSource.registerExistingAccount(
            email = "alice@example.com",
            password = "CorrectPassword123!"
        )

        val result = repository.signIn("alice@example.com", "WrongPassword!")

        assertTrue(result.isFailure)
        val authState = repository.authState.value
        assertTrue(authState is AuthState.Error)
        assertTrue((authState as AuthState.Error).error is AuthError.InvalidCredentials)
        assertNull(sessionRepository.getSession())
    }

    @Test
    fun signIn_networkFailure_failsWithNetworkFailure() = runTest(testDispatcher) {
        remoteAuthDataSource.simulateNetworkError = true

        val result = repository.signIn("alice@example.com", "Password123!")

        assertTrue(result.isFailure)
        val authState = repository.authState.value
        assertTrue(authState is AuthState.Error)
        assertTrue((authState as AuthState.Error).error is AuthError.NetworkFailure)
    }

    @Test
    fun signOut_clearsSession_andUpdatesAuthState() = runTest(testDispatcher) {
        // Sign in first
        remoteAuthDataSource.registerExistingAccount("alice@example.com", "Pass123!")
        repository.signIn("alice@example.com", "Pass123!")
        assertNotNull(sessionRepository.getSession())

        val signOutResult = repository.signOut()

        assertTrue(signOutResult.isSuccess)
        assertNull(sessionRepository.getSession())
        assertEquals(AuthState.SignedOut, repository.authState.value)
        assertEquals(1, remoteAuthDataSource.signedOutTokens.size)
    }

    @Test
    fun refreshSession_withActiveValidSession_restoresSignedInState() = runTest(testDispatcher) {
        sessionRepository.saveSession(
            AuthSession(
                userId = "user_valid_123",
                accessToken = "valid_token",
                expiresAtMillis = System.currentTimeMillis() + 60000L,
                userEmail = "valid@example.com"
            )
        )

        val result = repository.refreshSession()

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertNotNull(user)
        assertEquals("user_valid_123", user?.id)
        assertTrue(repository.authState.value is AuthState.SignedIn)
    }

    @Test
    fun refreshSession_withExpiredSession_refreshesUsingRefreshToken() = runTest(testDispatcher) {
        sessionRepository.saveSession(
            AuthSession(
                userId = "user_exp_123",
                accessToken = "expired_token",
                refreshToken = "refresh_token_abc",
                expiresAtMillis = System.currentTimeMillis() - 1000L, // Already expired
                userEmail = "exp@example.com"
            )
        )

        val result = repository.refreshSession()

        assertTrue(result.isSuccess)
        val refreshed = result.getOrThrow()
        assertNotNull(refreshed)
        assertTrue(sessionRepository.isSessionValid())
        assertTrue(repository.authState.value is AuthState.SignedIn)
    }

    @Test
    fun refreshSession_withExpiredSession_andInvalidRefreshToken_emitsSessionExpired() = runTest(testDispatcher) {
        remoteAuthDataSource.simulateExpiredRefreshToken = true
        sessionRepository.saveSession(
            AuthSession(
                userId = "user_exp_123",
                accessToken = "expired_token",
                refreshToken = "bad_refresh_token",
                expiresAtMillis = System.currentTimeMillis() - 1000L,
                userEmail = "exp@example.com"
            )
        )

        val result = repository.refreshSession()

        assertTrue(result.isFailure)
        assertNull(sessionRepository.getSession())
        assertTrue(repository.authState.value is AuthState.SessionExpired)
    }

    @Test
    fun refreshSession_withNoSession_returnsNullAndSignsOut() = runTest(testDispatcher) {
        val result = repository.refreshSession()

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
        assertEquals(AuthState.SignedOut, repository.authState.value)
    }

    @Test
    fun bindDevice_authenticatedUser_updatesRemoteLocalAndSession() = runTest(testDispatcher) {
        remoteAuthDataSource.registerExistingAccount("user@test.com", "Password123!", "user_bind_1")
        repository.signIn("user@test.com", "Password123!")

        val result = repository.bindDevice("new_device_token_xyz")

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("new_device_token_xyz", user.deviceBindingId)

        assertEquals("new_device_token_xyz", sessionRepository.getSession()?.deviceBindingId)
        assertEquals("new_device_token_xyz", remoteUserRepository.getRegisteredDeviceBindingId("user_bind_1").getOrNull())
        assertEquals("new_device_token_xyz", localUserRepository.getUser("user_bind_1")?.deviceBindingId)
    }

    @Test
    fun deleteAccount_authenticatedUser_cleansUpRemoteLocalAndSession() = runTest(testDispatcher) {
        remoteAuthDataSource.registerExistingAccount("delete_me@test.com", "Password123!", "del_user")
        repository.signIn("delete_me@test.com", "Password123!")

        val deleteResult = repository.deleteAccount()

        assertTrue(deleteResult.isSuccess)
        assertNull(sessionRepository.getSession())
        assertNull(localUserRepository.getUser("del_user"))
        assertNull(remoteUserRepository.fetchRemoteUserProfile("del_user").getOrNull())
        assertEquals(1, remoteAuthDataSource.deletedTokens.size)
        assertEquals(AuthState.SignedOut, repository.authState.value)
    }

    @Test
    fun verifyEmailOtp_validCode_authenticates_bindsDevice_andSavesSession() = runTest(testDispatcher) {
        val result = repository.verifyEmailOtp("otp_user@example.com", "123456")

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("otp_user@example.com", user.email)
        assertEquals("device_test_token_001", user.deviceBindingId)

        val authState = repository.authState.value
        assertTrue(authState is AuthState.SignedIn)
        assertEquals(user.id, (authState as AuthState.SignedIn).user.id)

        val session = sessionRepository.getSession()
        assertNotNull(session)
        assertEquals(user.id, session?.userId)
    }

    @Test
    fun verifyEmailOtp_invalidCode_failsWithDescriptiveError() = runTest(testDispatcher) {
        val result = repository.verifyEmailOtp("otp_user@example.com", "999999")

        assertTrue(result.isFailure)
        assertNull(sessionRepository.getSession())
        assertTrue(repository.authState.value is AuthState.Error)
    }

    @Test
    fun resendOtp_success_invokesRemoteSource() = runTest(testDispatcher) {
        val result = repository.resendOtp("resend_user@example.com")

        assertTrue(result.isSuccess)
        assertEquals(1, remoteAuthDataSource.resendCallCount)
    }

    @Test
    fun sendPasswordReset_success_invokesRemoteSource() = runTest(testDispatcher) {
        val result = repository.sendPasswordReset("reset_user@example.com")

        assertTrue(result.isSuccess)
        assertEquals("reset_user@example.com", remoteAuthDataSource.recoverEmailSentTo)
    }

    @Test
    fun signInWithGoogle_validIdToken_authenticatesAndSavesSession() = runTest(testDispatcher) {
        val result = repository.signInWithGoogle("valid_google_id_token_xyz")

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("google_user@example.com", user.email)

        assertTrue(repository.authState.value is AuthState.SignedIn)
        assertNotNull(sessionRepository.getSession())
    }
}

