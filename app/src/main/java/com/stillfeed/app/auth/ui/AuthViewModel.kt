package com.stillfeed.app.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stillfeed.app.auth.model.AuthError
import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.repository.AuthRepository
import com.stillfeed.app.auth.usecase.GetAuthStateUseCase
import com.stillfeed.app.auth.usecase.SignInUseCase
import com.stillfeed.app.auth.usecase.SignOutUseCase
import com.stillfeed.app.auth.usecase.SignUpUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing STILLFEED authentication interactions and state.
 *
 * Security rules:
 * - Passwords are wiped from state immediately after submission.
 * - Raw passwords and sensitive tokens are NEVER logged.
 */
class AuthViewModel(
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentAuthState: StateFlow<AuthState> = getAuthStateUseCase()

    init {
        // Observe repository auth state transitions
        viewModelScope.launch {
            currentAuthState.collect { state ->
                when (state) {
                    is AuthState.Loading, is AuthState.SigningIn, is AuthState.SigningUp -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }
                    is AuthState.NeedsEmailVerification -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentMode = AuthScreenMode.VERIFY_CODE,
                                emailInput = state.email,
                                errorMessage = null,
                                passwordInput = "",
                                confirmPasswordInput = "",
                                otpInput = ""
                            )
                        }
                    }
                    is AuthState.SignedIn -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                authenticatedUser = state.user,
                                errorMessage = null,
                                passwordInput = "",
                                confirmPasswordInput = "",
                                otpInput = ""
                            )
                        }
                    }
                    is AuthState.SignedOut -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                authenticatedUser = null,
                                passwordInput = "",
                                confirmPasswordInput = "",
                                otpInput = ""
                            )
                        }
                    }
                    is AuthState.SessionExpired -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentMode = AuthScreenMode.SIGN_IN,
                                isSignUpMode = false,
                                authenticatedUser = null,
                                errorMessage = state.message,
                                passwordInput = "",
                                confirmPasswordInput = "",
                                otpInput = ""
                            )
                        }
                    }
                    is AuthState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = state.error.userMessage,
                                passwordInput = "",
                                confirmPasswordInput = ""
                            )
                        }
                    }
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(emailInput = email, errorMessage = null, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null, passwordError = null) }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update { it.copy(confirmPasswordInput = confirmPassword, errorMessage = null, confirmPasswordError = null) }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayNameInput = name, errorMessage = null) }
    }

    fun onOtpChanged(otp: String) {
        val filtered = otp.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otpInput = filtered, errorMessage = null) }
        if (filtered.length == 6) {
            submitVerifyOtp(filtered)
        }
    }

    fun toggleAuthMode() {
        _uiState.update {
            val newSignUp = !it.isSignUpMode
            it.copy(
                isSignUpMode = newSignUp,
                currentMode = if (newSignUp) AuthScreenMode.CREATE_ACCOUNT else AuthScreenMode.SIGN_IN,
                errorMessage = null,
                passwordError = null,
                confirmPasswordError = null,
                emailError = null,
                passwordInput = "",
                confirmPasswordInput = "",
                otpInput = ""
            )
        }
    }

    fun navigateToCreateAccount() {
        _uiState.update {
            it.copy(
                currentMode = AuthScreenMode.CREATE_ACCOUNT,
                isSignUpMode = true,
                errorMessage = null,
                passwordError = null,
                confirmPasswordError = null,
                emailError = null,
                passwordInput = "",
                confirmPasswordInput = "",
                otpInput = ""
            )
        }
    }

    fun navigateToSignIn() {
        _uiState.update {
            it.copy(
                currentMode = AuthScreenMode.SIGN_IN,
                isSignUpMode = false,
                errorMessage = null,
                passwordError = null,
                confirmPasswordError = null,
                emailError = null,
                passwordInput = "",
                confirmPasswordInput = "",
                otpInput = ""
            )
        }
    }

    fun navigateToForgotPassword() {
        _uiState.update {
            it.copy(
                currentMode = AuthScreenMode.FORGOT_PASSWORD,
                errorMessage = null,
                emailError = null
            )
        }
    }

    fun navigateToVerifyCode(email: String = _uiState.value.emailInput) {
        _uiState.update {
            it.copy(
                currentMode = AuthScreenMode.VERIFY_CODE,
                emailInput = email,
                errorMessage = null,
                otpInput = ""
            )
        }
    }

    fun submitSignIn() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        val email = currentState.emailInput.trim()
        val password = currentState.passwordInput

        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address.", passwordInput = "") }
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            _uiState.update { it.copy(errorMessage = AuthError.InvalidEmail().userMessage, passwordInput = "") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.", passwordInput = "") }
            return
        }

        viewModelScope.launch {
            val result = signInUseCase(email, password)
            if (result.isFailure) {
                val msg = result.exceptionOrNull()?.message
                    ?: "Email or password is incorrect. Check them and try again."
                _uiState.update { it.copy(errorMessage = msg, passwordInput = "") }
            }
        }
    }

    fun submitSignUp() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        val email = currentState.emailInput.trim()
        val password = currentState.passwordInput
        val confirmPassword = currentState.confirmPasswordInput
        val displayName = currentState.displayNameInput

        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address.", emailError = "Please enter your email.", passwordInput = "", confirmPasswordInput = "") }
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            _uiState.update { it.copy(errorMessage = AuthError.InvalidEmail().userMessage, emailError = AuthError.InvalidEmail().userMessage, passwordInput = "", confirmPasswordInput = "") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.", passwordError = "Please enter your password.", passwordInput = "", confirmPasswordInput = "") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = AuthError.WeakPassword().userMessage, passwordError = "Use at least 10 characters.", passwordInput = "", confirmPasswordInput = "") }
            return
        }
        if (confirmPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please confirm your password.", confirmPasswordError = "Please confirm your password.", passwordInput = "", confirmPasswordInput = "") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "The passwords do not match. Please check and try again.", confirmPasswordError = "Passwords don’t match.", passwordInput = "", confirmPasswordInput = "") }
            return
        }

        viewModelScope.launch {
            val result = signUpUseCase(email, password, displayName)
            if (result.isFailure) {
                val msg = result.exceptionOrNull()?.message
                    ?: AuthError.AccountCreationFailed().userMessage
                _uiState.update { it.copy(errorMessage = msg, passwordInput = "", confirmPasswordInput = "") }
            }
        }
    }

    fun submitVerifyOtp(code: String = _uiState.value.otpInput) {
        val currentState = _uiState.value
        if (currentState.isLoading) return
        val email = currentState.emailInput.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email.") }
            return
        }
        if (code.length < 6) {
            _uiState.update { it.copy(errorMessage = "Please enter all 6 digits.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.verifyEmailOtp(email, code)
            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message
                    ?: "That code is incorrect or has expired. Request a new code and try again."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg,
                        otpInput = ""
                    )
                }
            }
        }
    }

    fun resendOtp() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.resendCountdownSeconds > 0) return
        val email = currentState.emailInput.trim()
        if (email.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.resendOtp(email)
            if (result.isSuccess) {
                startResendCountdown(30)
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to resend code."
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    private fun startResendCountdown(seconds: Int = 30) {
        viewModelScope.launch {
            for (sec in seconds downTo 0) {
                _uiState.update { it.copy(resendCountdownSeconds = sec) }
                if (sec > 0) delay(1000)
            }
        }
    }

    fun submitForgotPassword() {
        val currentState = _uiState.value
        if (currentState.isLoading) return
        val email = currentState.emailInput.trim()
        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
            _uiState.update { it.copy(emailError = "Please enter a valid email address.", errorMessage = "Please enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, emailError = null) }
            val result = authRepository.sendPasswordReset(email)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentMode = AuthScreenMode.RESET_LINK_SENT,
                        resetEmailSentTo = email,
                        errorMessage = null
                    )
                }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Unable to send reset link."
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    fun submitGoogleSignIn(idToken: String? = null) {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        if (idToken.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Google Sign-In is awaiting Google OAuth configuration in the Supabase Dashboard. Please sign in with Email or configure Google provider credentials."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isFailure) {
                val msg = result.exceptionOrNull()?.message ?: "Google sign-in failed."
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    fun submit() {
        if (_uiState.value.isSignUpMode) {
            submitSignUp()
        } else {
            submitSignIn()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
        }
    }

    fun bindCurrentDevice(deviceId: String) {
        viewModelScope.launch {
            authRepository.bindDevice(deviceId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, passwordError = null, confirmPasswordError = null, emailError = null) }
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(
                        getAuthStateUseCase = GetAuthStateUseCase(authRepository),
                        signInUseCase = SignInUseCase(authRepository),
                        signUpUseCase = SignUpUseCase(authRepository),
                        signOutUseCase = SignOutUseCase(authRepository),
                        authRepository = authRepository
                    ) as T
                }
            }
    }
}
