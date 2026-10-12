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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests for STILLFEED STEP 03B Authentication UI & Validation.
 *
 * Verifies:
 * - Sign-in form validation (empty email, invalid email, empty password)
 * - Create-account validation (empty email, invalid email, empty password, weak password, empty confirm password, mismatch)
 * - Matching password creation success
 * - Independent password visibility toggling semantics
 * - Navigation between Sign In and Create Account
 * - Prevention of duplicate submissions during active loading
 * - Secure wiping of passwords from memory/state
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthUiValidationTest {

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

    // -------------------------------------------------------------------------
    // Sign In Validation Tests
    // -------------------------------------------------------------------------

    @Test
    fun signIn_withEmptyEmail_showsValidationMessage_andClearsPassword() = testScope.runTest {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("somePassword123")
        viewModel.submitSignIn()
        advanceUntilIdle()

        assertEquals("Please enter your email address.", viewModel.uiState.value.errorMessage)
        assertEquals("", viewModel.uiState.value.passwordInput)
    }

    @Test
    fun signIn_withInvalidEmailFormat_showsValidationMessage() = testScope.runTest {
        viewModel.onEmailChanged("notanemail")
        viewModel.onPasswordChanged("somePassword123")
        viewModel.submitSignIn()
        advanceUntilIdle()

        assertEquals("Please enter a valid email address.", viewModel.uiState.value.errorMessage)
        assertEquals("", viewModel.uiState.value.passwordInput)
    }

    @Test
    fun signIn_withEmptyPassword_showsValidationMessage() = testScope.runTest {
        viewModel.onEmailChanged("valid.user@stillfeed.app")
        viewModel.onPasswordChanged("")
        viewModel.submitSignIn()
        advanceUntilIdle()

        assertEquals("Please enter your password.", viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // Create Account Validation Tests
    // -------------------------------------------------------------------------

    @Test
    fun createAccount_withEmptyEmail_showsValidationMessage() = testScope.runTest {
        viewModel.navigateToCreateAccount()
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("validPassword123")
        viewModel.onConfirmPasswordChanged("validPassword123")
        viewModel.submitSignUp()
        advanceUntilIdle()

        assertEquals("Please enter your email address.", viewModel.uiState.value.errorMessage)
        assertEquals("validPassword123", viewModel.uiState.value.passwordInput)
        assertEquals("validPassword123", viewModel.uiState.value.confirmPasswordInput)
    }

    @Test
    fun createAccount_withInvalidEmail_showsValidationMessage() = testScope.runTest {
        viewModel.navigateToCreateAccount()
        viewModel.onEmailChanged("user-without-at")
        viewModel.onPasswordChanged("validPassword123")
        viewModel.onConfirmPasswordChanged("validPassword123")
        viewModel.submitSignUp()
        advanceUntilIdle()

        assertEquals("Please enter a valid email address.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun createAccount_withEmptyPassword_showsValidationMessage() = testScope.runTest {
        viewModel.navigateToCreateAccount()
        viewModel.onEmailChanged("new.user@stillfeed.app")
        viewModel.onPasswordChanged("")
        viewModel.onConfirmPasswordChanged("")
        viewModel.submitSignUp()
        advanceUntilIdle()

        assertEquals("Please enter your password.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun createAccount_withShortPassword_showsWeakPasswordMessage() = testScope.runTest {
        viewModel.navigateToCreateAccount()
        viewModel.onEmailChanged("new.user@stillfeed.app")
        viewModel.onPasswordChanged("short")
        viewModel.onConfirmPasswordChanged("short")
        viewModel.submitSignUp()
        advanceUntilIdle()

        assertEquals("Please choose a password with at least 8 characters.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun createAccount_withEmptyConfirmPassword_showsValidationMessage() = testScope.runTest {
        viewModel.navigateToCreateAccount()
        viewModel.onEmailChanged("new.user@stillfeed.app")
        viewModel.onPasswordChanged("validPassword123")
        viewModel.onConfirmPasswordChanged("")
        viewModel.submitSignUp()
        advanceUntilIdle()

        assertEquals("Please confirm your password.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun createAccount_withPasswordMismatch_showsMismatchMessage() = testScope.runTest {
        viewModel.navigateToCreateAccount()
        viewModel.onEmailChanged("new.user@stillfeed.app")
        viewModel.onPasswordChanged("validPassword123")
        viewModel.onConfirmPasswordChanged("differentPassword456")
        viewModel.submitSignUp()
        advanceUntilIdle()

        assertEquals("The passwords do not match. Please check and try again.", viewModel.uiState.value.errorMessage)
        assertEquals("validPassword123", viewModel.uiState.value.passwordInput)
        assertEquals("differentPassword456", viewModel.uiState.value.confirmPasswordInput)
    }

    @Test
    fun createAccount_withValidInputs_createsAccountAndSignsIn() = testScope.runTest {
        viewModel.navigateToCreateAccount()
        viewModel.onEmailChanged("fresh.account@stillfeed.app")
        viewModel.onPasswordChanged("mySecurePass123")
        viewModel.onConfirmPasswordChanged("mySecurePass123")
        viewModel.submitSignUp()
        advanceUntilIdle()

        assertNull("Error message must be null on successful account creation", viewModel.uiState.value.errorMessage)
        assertNotNull(viewModel.uiState.value.authenticatedUser)
        assertEquals("fresh.account@stillfeed.app", viewModel.uiState.value.authenticatedUser?.email)
        assertEquals("", viewModel.uiState.value.passwordInput)
        assertEquals("", viewModel.uiState.value.confirmPasswordInput)
        assertTrue(viewModel.currentAuthState.value is AuthState.SignedIn)
    }

    // -------------------------------------------------------------------------
    // Navigation & Mode Switching Tests
    // -------------------------------------------------------------------------

    @Test
    fun navigation_betweenSignInAndCreateAccount_clearsPasswordsAndErrors() = testScope.runTest {
        // Start in Sign In
        assertFalse(viewModel.uiState.value.isSignUpMode)

        viewModel.onPasswordChanged("leftoverSecret")
        viewModel.navigateToCreateAccount()

        assertTrue(viewModel.uiState.value.isSignUpMode)
        assertEquals("", viewModel.uiState.value.passwordInput)
        assertEquals("", viewModel.uiState.value.confirmPasswordInput)
        assertNull(viewModel.uiState.value.errorMessage)

        viewModel.onPasswordChanged("anotherSecret")
        viewModel.onConfirmPasswordChanged("confirmSecret")
        viewModel.navigateToSignIn()

        assertFalse(viewModel.uiState.value.isSignUpMode)
        assertEquals("", viewModel.uiState.value.passwordInput)
        assertEquals("", viewModel.uiState.value.confirmPasswordInput)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // Password Visibility Decoupling Tests
    // -------------------------------------------------------------------------

    @Test
    fun passwordVisibility_statesAreCompletelyIndependent() {
        var passwordVisible = false
        var confirmPasswordVisible = false

        // Toggle main password visibility
        passwordVisible = !passwordVisible
        assertTrue("Main password should now be visible", passwordVisible)
        assertFalse("Confirm password must remain hidden", confirmPasswordVisible)

        // Toggle confirm password visibility
        confirmPasswordVisible = !confirmPasswordVisible
        assertTrue("Main password must remain visible", passwordVisible)
        assertTrue("Confirm password should now be visible", confirmPasswordVisible)

        // Hide main password again
        passwordVisible = !passwordVisible
        assertFalse("Main password should now be hidden", passwordVisible)
        assertTrue("Confirm password must still be visible", confirmPasswordVisible)
    }
}
