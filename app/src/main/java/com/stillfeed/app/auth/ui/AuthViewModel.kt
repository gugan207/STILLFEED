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
                    is AuthState.SignedIn -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                authenticatedUser = state.user,
                                errorMessage = null,
                                passwordInput = "", // Clear password from memory
                                confirmPasswordInput = ""
                            )
                        }
                    }
                    is AuthState.SignedOut -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                authenticatedUser = null,
                                passwordInput = "",
                                confirmPasswordInput = ""
                            )
                        }
                    }
                    is AuthState.SessionExpired -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                authenticatedUser = null,
                                errorMessage = state.message,
                                passwordInput = "",
                                confirmPasswordInput = ""
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
        _uiState.update { it.copy(emailInput = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update { it.copy(confirmPasswordInput = confirmPassword, errorMessage = null) }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayNameInput = name, errorMessage = null) }
    }

    fun toggleAuthMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null,
                passwordInput = "",
                confirmPasswordInput = ""
            )
        }
    }

    fun navigateToCreateAccount() {
        _uiState.update {
            it.copy(
                isSignUpMode = true,
                errorMessage = null,
                passwordInput = "",
                confirmPasswordInput = ""
            )
        }
    }

    fun navigateToSignIn() {
        _uiState.update {
            it.copy(
                isSignUpMode = false,
                errorMessage = null,
                passwordInput = "",
                confirmPasswordInput = ""
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
                    ?: AuthError.InvalidCredentials().userMessage
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
            _uiState.update { it.copy(errorMessage = "Please enter your email address.", passwordInput = "", confirmPasswordInput = "") }
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            _uiState.update { it.copy(errorMessage = AuthError.InvalidEmail().userMessage, passwordInput = "", confirmPasswordInput = "") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.", passwordInput = "", confirmPasswordInput = "") }
            return
        }
        if (password.length < 8) {
            _uiState.update { it.copy(errorMessage = AuthError.WeakPassword().userMessage, passwordInput = "", confirmPasswordInput = "") }
            return
        }
        if (confirmPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please confirm your password.", passwordInput = "", confirmPasswordInput = "") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "The passwords do not match. Please check and try again.", passwordInput = "", confirmPasswordInput = "") }
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
        _uiState.update { it.copy(errorMessage = null) }
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
