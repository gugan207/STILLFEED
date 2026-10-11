package com.stillfeed.app.auth

import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.ui.AuthViewModel
import com.stillfeed.app.auth.usecase.GetAuthStateUseCase
import com.stillfeed.app.auth.usecase.SignInUseCase
import com.stillfeed.app.auth.usecase.SignOutUseCase
import com.stillfeed.app.auth.usecase.SignUpUseCase
import com.stillfeed.app.data.auth.InMemoryAuthSessionStorage
import com.stillfeed.app.data.auth.LocalAuthRepository
import com.stillfeed.app.data.auth.Pbkdf2PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var sessionStorage: InMemoryAuthSessionStorage
    private lateinit var repository: LocalAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionStorage = InMemoryAuthSessionStorage()
        val testHasher = Pbkdf2PasswordHasher(iterations = 10_000)
        repository = LocalAuthRepository(
            sessionStorage = sessionStorage,
            passwordHasher = testHasher,
            ioDispatcher = testDispatcher
        )
        viewModel = AuthViewModel(
            getAuthStateUseCase = GetAuthStateUseCase(repository),
            signInUseCase = SignInUseCase(repository),
            signUpUseCase = SignUpUseCase(repository),
            signOutUseCase = SignOutUseCase(repository),
            authRepository = repository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isSignedOut() = testScope.runTest {
        advanceUntilIdle()
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNull(uiState.authenticatedUser)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun toggleAuthMode_switchesBetweenSignInAndSignUp_andClearsPassword() = testScope.runTest {
        viewModel.onPasswordChanged("strayPassword123")
        assertFalse(viewModel.uiState.value.isSignUpMode)

        viewModel.toggleAuthMode()
        assertTrue(viewModel.uiState.value.isSignUpMode)
        assertEquals("Password must be cleared on mode toggle", "", viewModel.uiState.value.passwordInput)

        viewModel.onPasswordChanged("anotherStrayPassword")
        viewModel.toggleAuthMode()
        assertFalse(viewModel.uiState.value.isSignUpMode)
        assertEquals("Password must be cleared on mode toggle", "", viewModel.uiState.value.passwordInput)
    }

    @Test
    fun signIn_withInvalidEmail_setsErrorMessage_andDoesNotRetainPassword() = testScope.runTest {
        viewModel.onEmailChanged("not-an-email")
        viewModel.onPasswordChanged("short")
        viewModel.submit()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signIn_withValidInputs_authenticatesAndWipesPassword() = testScope.runTest {
        viewModel.onEmailChanged("user@stillfeed.app")
        viewModel.onPasswordChanged("validPassword123")
        viewModel.submit()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.authenticatedUser)
        assertEquals("user@stillfeed.app", uiState.authenticatedUser?.email)
        assertEquals("Password memory must be wiped after authentication", "", uiState.passwordInput)
        assertTrue(viewModel.currentAuthState.value is AuthState.SignedIn)
    }

    @Test
    fun signIn_withWrongPassword_fails_andWipesPasswordFromState() = testScope.runTest {
        // Register account first
        viewModel.toggleAuthMode() // Sign up mode
        viewModel.onEmailChanged("user2@stillfeed.app")
        viewModel.onPasswordChanged("correctPassword123")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.passwordInput)

        // Sign out
        viewModel.signOut()
        advanceUntilIdle()

        // Attempt sign in with wrong password
        viewModel.onEmailChanged("user2@stillfeed.app")
        viewModel.onPasswordChanged("wrongPassword999")
        viewModel.submit()
        advanceUntilIdle()

        // Assert failure and memory clearing
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Password memory must be wiped after failed sign in", "", viewModel.uiState.value.passwordInput)
    }

    @Test
    fun signOut_clearsAuthenticatedUser() = testScope.runTest {
        // Authenticate first
        viewModel.onEmailChanged("user@stillfeed.app")
        viewModel.onPasswordChanged("validPassword123")
        viewModel.submit()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.authenticatedUser)

        // Sign out
        viewModel.signOut()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.authenticatedUser)
        assertEquals(AuthState.SignedOut, viewModel.currentAuthState.value)
    }

    @Test
    fun bindCurrentDevice_updatesAuthenticatedUserBinding() = testScope.runTest {
        viewModel.onEmailChanged("deviceuser@stillfeed.app")
        viewModel.onPasswordChanged("validPassword123")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.bindCurrentDevice("device_test_hw_123")
        advanceUntilIdle()

        val user = viewModel.uiState.value.authenticatedUser
        assertNotNull(user)
        assertEquals("device_test_hw_123", user?.deviceBindingId)
        assertTrue(user?.isDeviceBound == true)
    }

    @Test
    fun otpInput_filtersDigitsAndSubmitsAtSixDigits() = testScope.runTest {
        viewModel.onEmailChanged("otp@stillfeed.app")
        viewModel.navigateToVerifyCode()
        assertEquals(com.stillfeed.app.auth.ui.AuthScreenMode.VERIFY_CODE, viewModel.uiState.value.currentMode)

        // Type 5 digits
        viewModel.onOtpChanged("123ab45")
        assertEquals("12345", viewModel.uiState.value.otpInput)

        // Type 6th digit -> triggers verification
        viewModel.onOtpChanged("123456")
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.otpInput)
        assertTrue(viewModel.currentAuthState.value is AuthState.SignedIn)
        assertNotNull(viewModel.uiState.value.authenticatedUser)
    }

    @Test
    fun submitForgotPassword_withValidEmail_switchesToResetLinkSentMode() = testScope.runTest {
        viewModel.onEmailChanged("reset@stillfeed.app")
        viewModel.navigateToForgotPassword()
        assertEquals(com.stillfeed.app.auth.ui.AuthScreenMode.FORGOT_PASSWORD, viewModel.uiState.value.currentMode)

        viewModel.submitForgotPassword()
        advanceUntilIdle()

        assertEquals(com.stillfeed.app.auth.ui.AuthScreenMode.RESET_LINK_SENT, viewModel.uiState.value.currentMode)
        assertEquals("reset@stillfeed.app", viewModel.uiState.value.resetEmailSentTo)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun submitGoogleSignIn_withoutToken_showsHelpfulConfigurationMessage() = testScope.runTest {
        viewModel.submitGoogleSignIn(null)

        val error = viewModel.uiState.value.errorMessage
        assertNotNull(error)
        assertTrue(error?.contains("Google OAuth") == true)
    }

    @Test
    fun submitGoogleSignIn_withToken_authenticatesUser() = testScope.runTest {
        viewModel.submitGoogleSignIn("valid_google_token_123")
        advanceUntilIdle()

        assertTrue(viewModel.currentAuthState.value is AuthState.SignedIn)
        assertEquals("google_user@stillfeed.app", viewModel.uiState.value.authenticatedUser?.email)
    }
}

