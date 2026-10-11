package com.stillfeed.app.auth.usecase

import com.stillfeed.app.auth.model.AuthError
import com.stillfeed.app.auth.model.AuthUser
import com.stillfeed.app.auth.repository.AuthRepository

class SignInUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: CharSequence): Result<AuthUser> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return Result.failure(IllegalArgumentException(AuthError.InvalidEmail().userMessage))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException(AuthError.InvalidCredentials().userMessage))
        }
        return authRepository.signIn(trimmedEmail, password)
    }
}
