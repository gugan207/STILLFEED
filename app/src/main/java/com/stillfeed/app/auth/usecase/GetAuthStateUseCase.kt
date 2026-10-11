package com.stillfeed.app.auth.usecase

import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Observes the live authentication state of the application.
 */
class GetAuthStateUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): StateFlow<AuthState> = authRepository.authState
}
