package com.stillfeed.app.auth.ui

import com.stillfeed.app.auth.model.AuthUser

/**
 * Visual authentication modes matching Figma flows:
 * - 00A: SIGN_IN
 * - 00B: CREATE_ACCOUNT
 * - 00C: VERIFY_CODE
 * - 00D: FORGOT_PASSWORD
 * - 00E: RESET_LINK_SENT
 */
enum class AuthScreenMode {
    SIGN_IN,
    CREATE_ACCOUNT,
    VERIFY_CODE,
    FORGOT_PASSWORD,
    RESET_LINK_SENT
}

/**
 * UI State for STILLFEED authentication screens and components.
 */
data class AuthUiState(
    val currentMode: AuthScreenMode = AuthScreenMode.SIGN_IN,
    val emailInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val otpInput: String = "",
    val displayNameInput: String = "",
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val emailError: String? = null,
    val resendCountdownSeconds: Int = 0,
    val resetEmailSentTo: String? = null,
    val authenticatedUser: AuthUser? = null
) {
    val isInputValid: Boolean
        get() = emailInput.isNotBlank() && passwordInput.isNotBlank() &&
            (currentMode != AuthScreenMode.CREATE_ACCOUNT || (passwordInput.length >= 10 && passwordInput == confirmPasswordInput))
}
