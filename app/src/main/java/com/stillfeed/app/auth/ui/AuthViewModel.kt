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

    fun onNewPasswordChanged(password: String) {
        _uiState.update { it.copy(newPasswordInput = password, errorMessage = null, newPasswordError = null) }
    }

    fun onConfirmNewPasswordChanged(confirmPassword: String) {
        _uiState.update { it.copy(confirmNewPasswordInput = confirmPassword, errorMessage = null, confirmNewPasswordError = null) }
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
        authRepository.clearRecoverySession()
        _uiState.update {
            it.copy(
                currentMode = AuthScreenMode.SIGN_IN,
                isSignUpMode = false,
                errorMessage = null,
                passwordError = null,
                confirmPasswordError = null,
                emailError = null,
                newPasswordError = null,
                confirmNewPasswordError = null,
                newPasswordInput = "",
                confirmNewPasswordInput = "",
                isResetPasswordSuccess = false,
                resetPasswordSuccessMessage = null,
                resetLinkResendFeedback = null,
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
                emailError = null,
                resetLinkResendFeedback = null
            )
        }
    }

    fun navigateToResetPassword() {
        _uiState.update {
            it.copy(
                currentMode = AuthScreenMode.RESET_PASSWORD,
                errorMessage = null,
                newPasswordError = null,
                confirmNewPasswordError = null
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
            _uiState.update { it.copy(errorMessage = "Please enter your email address.", emailError = "Please enter your email.") }
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            _uiState.update { it.copy(errorMessage = AuthError.InvalidEmail().userMessage, emailError = AuthError.InvalidEmail().userMessage) }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.", passwordError = "Please enter your password.") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = AuthError.WeakPassword().userMessage, passwordError = "Use at least 8 characters.") }
            return
        }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasSymbolOrDigit = password.any { !it.isLetter() }
        if (!hasUppercase || !hasSymbolOrDigit) {
            _uiState.update {
                it.copy(
                    errorMessage = "Password must include at least 1 uppercase letter and 1 number or symbol.",
                    passwordError = "Include 1 capital letter & 1 symbol/number."
                )
            }
            return
        }
        if (confirmPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please confirm your password.", confirmPasswordError = "Please confirm your password.") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "The passwords do not match. Please check and try again.", confirmPasswordError = "Passwords don’t match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = signUpUseCase(email, password, displayName)
            if (result.isFailure) {
                val msg = result.exceptionOrNull()?.message
                    ?: AuthError.AccountCreationFailed().userMessage
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            } else {
                _uiState.update { it.copy(isLoading = false, passwordInput = "", confirmPasswordInput = "") }
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
            val result = authRepository.sendPasswordReset(email, redirectTo = "stillfeed://reset-password")
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentMode = AuthScreenMode.RESET_LINK_SENT,
                        resetEmailSentTo = email,
                        errorMessage = null,
                        resetLinkResendFeedback = null
                    )
                }
                startResendCountdown(60)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Unable to send reset link."
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    fun resendForgotPassword() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.resendCountdownSeconds > 0) return
        val email = currentState.resetEmailSentTo ?: currentState.emailInput.trim()
        if (email.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, resetLinkResendFeedback = null) }
            val result = authRepository.sendPasswordReset(email, redirectTo = "stillfeed://reset-password")
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        resetLinkResendFeedback = "A new reset link has been sent to your email."
                    )
                }
                startResendCountdown(60)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Unable to resend reset link."
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    fun handleDeepLink(uriString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.handleRecoveryLink(uriString)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentMode = AuthScreenMode.RESET_PASSWORD,
                        errorMessage = null,
                        newPasswordInput = "",
                        confirmNewPasswordInput = "",
                        newPasswordError = null,
                        confirmNewPasswordError = null,
                        isResetPasswordSuccess = false,
                        resetPasswordSuccessMessage = null
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message
                    ?: "This password reset link is invalid or has expired."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentMode = AuthScreenMode.FORGOT_PASSWORD,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    fun submitResetPassword() {
        val currentState = _uiState.value
        if (currentState.isLoading) return
        val newPassword = currentState.newPasswordInput
        val confirmPassword = currentState.confirmNewPasswordInput

        if (newPassword.isBlank()) {
            _uiState.update { it.copy(newPasswordError = "Please enter a new password.", errorMessage = "Please enter a new password.") }
            return
        }
        if (newPassword.length < 8) {
            _uiState.update { it.copy(newPasswordError = "Use at least 8 characters.", errorMessage = "Please choose a password with at least 8 characters.") }
            return
        }
        if (!newPassword.any { it.isUpperCase() } || !newPassword.any { !it.isLetter() }) {
            _uiState.update {
                it.copy(
                    newPasswordError = "Must include 1 uppercase and 1 number/symbol.",
                    errorMessage = "Password must include at least 1 uppercase letter and 1 number or symbol."
                )
            }
            return
        }
        if (confirmPassword.isBlank()) {
            _uiState.update { it.copy(confirmNewPasswordError = "Please confirm your password.", errorMessage = "Please confirm your password.") }
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.update {
                it.copy(
                    confirmNewPasswordError = "Passwords don’t match.",
                    errorMessage = "The passwords do not match. Please check and try again."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.resetPassword(newPassword)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isResetPasswordSuccess = true,
                        resetPasswordSuccessMessage = "Your password has been reset successfully! Please sign in with your new password.",
                        newPasswordInput = "",
                        confirmNewPasswordInput = "",
                        errorMessage = null
                    )
                }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Unable to reset password."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = msg,
                        newPasswordInput = "",
                        confirmNewPasswordInput = ""
                    )
                }
            }
        }
    }

    fun submitGoogleSignIn(idToken: String? = null) {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        if (idToken.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Google Sign-In was cancelled or Google OAuth token was not received. Please try again."
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
            if (_uiState.value.confirmPasswordInput.isBlank() && _uiState.value.passwordInput.isNotBlank()) {
                _uiState.update { it.copy(confirmPasswordInput = it.passwordInput) }
            }
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

    fun showErrorMessage(message: String) {
        _uiState.update { it.copy(errorMessage = message, isLoading = false) }
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
