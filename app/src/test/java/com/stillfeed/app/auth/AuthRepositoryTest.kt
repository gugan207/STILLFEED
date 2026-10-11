package com.stillfeed.app.auth

import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.model.AuthUser
import com.stillfeed.app.data.auth.InMemoryAuthSessionStorage
import com.stillfeed.app.data.auth.LocalAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var sessionStorage: InMemoryAuthSessionStorage
    private lateinit var repository: LocalAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionStorage = InMemoryAuthSessionStorage()
        val testHasher = com.stillfeed.app.data.auth.Pbkdf2PasswordHasher(iterations = 10_000)
        repository = LocalAuthRepository(
            sessionStorage = sessionStorage,
            passwordHasher = testHasher,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_withoutSession_isSignedOut() = testScope.runTest {
        val state = repository.authState.value
        assertEquals(AuthState.SignedOut, state)
    }

    @Test
    fun initialState_withExistingSession_restoresSignedIn() = testScope.runTest {
        val savedUser = AuthUser(
            id = "user_existing_123",
            email = "saved@stillfeed.app",
            displayName = "Saved User"
        )
        sessionStorage.saveSession(savedUser, "token_xyz")

        val newRepository = LocalAuthRepository(
            sessionStorage = sessionStorage,
            ioDispatcher = testDispatcher
        )
        val state = newRepository.authState.value
        assertTrue("Expected SignedIn state", state is AuthState.SignedIn)
        assertEquals(savedUser.id, (state as AuthState.SignedIn).user.id)
    }

    @Test
    fun signIn_withValidCredentials_succeedsAndUpdatesState() = testScope.runTest {
        val result = repository.signIn("test@stillfeed.app", "validPassword123")
        assertTrue(result.isSuccess)

        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("test@stillfeed.app", user?.email)

        val state = repository.authState.value
        assertTrue(state is AuthState.SignedIn)
        assertEquals(user?.id, (state as AuthState.SignedIn).user.id)
        assertNotNull(sessionStorage.getSessionToken())
    }

    @Test
    fun signIn_withWrongPassword_failsAndEmitsError() = testScope.runTest {
        // Sign up first
        val signUpResult = repository.signUp("user@stillfeed.app", "correctPassword123")
        assertTrue(signUpResult.isSuccess)

        // Attempt sign in with wrong password
        val signInResult = repository.signIn("user@stillfeed.app", "wrongPassword999")
        assertTrue(signInResult.isFailure)

        val state = repository.authState.value
        assertTrue("Expected error state on wrong password", state is AuthState.Error)
    }

    @Test
    fun signUp_withExistingEmail_fails() = testScope.runTest {
        val first = repository.signUp("repeat@stillfeed.app", "password123")
        assertTrue(first.isSuccess)

        val second = repository.signUp("repeat@stillfeed.app", "password456")
        assertTrue(second.isFailure)
        assertTrue(repository.authState.value is AuthState.Error)
    }

    @Test
    fun signOut_clearsSessionAndEmitsSignedOut() = testScope.runTest {
        repository.signIn("active@stillfeed.app", "password123")
        assertTrue(repository.authState.value is AuthState.SignedIn)

        val signOutResult = repository.signOut()
        assertTrue(signOutResult.isSuccess)
        assertEquals(AuthState.SignedOut, repository.authState.value)
        assertNull(sessionStorage.getSessionToken())
    }

    @Test
    fun bindDevice_updatesUserAndStorage() = testScope.runTest {
        repository.signIn("device@stillfeed.app", "password123")
        val bindResult = repository.bindDevice("device_test_hw_99")
        assertTrue(bindResult.isSuccess)

        val boundUser = bindResult.getOrNull()
        assertNotNull(boundUser)
        assertEquals("device_test_hw_99", boundUser?.deviceBindingId)
        assertTrue(boundUser?.isDeviceBound == true)
        assertEquals("device_test_hw_99", sessionStorage.getDeviceBinding())
    }
}
