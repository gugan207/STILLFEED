package com.stillfeed.app.auth.usecase

import com.stillfeed.app.auth.repository.AuthRepository

class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.signOut()
}
