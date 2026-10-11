package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing active authentication tokens and sessions.
 *
 * Security rules:
 * - Opaque bearer tokens only; never plaintext credentials.
 * - Reactive flow observation for automatic session expiration handling.
 */
interface SessionRepository {
    fun observeSession(): Flow<AuthSession?>
    suspend fun getSession(): AuthSession?
    suspend fun saveSession(session: AuthSession)
    suspend fun clearSession()
    suspend fun isSessionValid(): Boolean
}
