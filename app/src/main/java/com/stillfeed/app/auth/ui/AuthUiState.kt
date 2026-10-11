package com.stillfeed.app.auth.ui

import com.stillfeed.app.auth.model.AuthUser

/**
 * UI State for STILLFEED authentication screen.
 */
data class AuthUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val displayNameInput: String = "",
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticatedUser: AuthUser? = null
) {
    val isInputValid: Boolean
        get() = emailInput.isNotBlank() && passwordInput.isNotBlank() &&
            (!isSignUpMode || (passwordInput.length >= 8 && passwordInput == confirmPasswordInput))
}
