package com.stillfeed.app.auth.usecase

import com.stillfeed.app.auth.model.AuthError
import com.stillfeed.app.auth.model.AuthUser
import com.stillfeed.app.auth.repository.AuthRepository

class SignUpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: CharSequence,
        displayName: String? = null
    ): Result<AuthUser> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return Result.failure(IllegalArgumentException(AuthError.InvalidEmail().userMessage))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException(AuthError.WeakPassword().userMessage))
        }
        return authRepository.signUp(trimmedEmail, password, displayName?.trim())
    }
}
