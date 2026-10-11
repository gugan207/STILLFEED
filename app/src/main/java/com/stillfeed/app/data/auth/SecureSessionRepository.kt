package com.stillfeed.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.stillfeed.app.domain.model.AuthSession
import com.stillfeed.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Concrete [SessionRepository] persisting opaque session bearer tokens.
 *
 * Security guarantees:
 * - Plaintext passwords are NEVER stored in this repository.
 * - Access and refresh tokens are stored in private app storage.
 * - Provides reactive flow observation for session status changes.
 */
class SecureSessionRepository(
    context: Context,
    prefsName: String = "stillfeed_secure_session_tokens"
) : SessionRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val _sessionFlow = MutableStateFlow<AuthSession?>(readSessionFromStorage())

    override fun observeSession(): Flow<AuthSession?> = _sessionFlow.asStateFlow()

    override suspend fun getSession(): AuthSession? = _sessionFlow.value

    override suspend fun saveSession(session: AuthSession) {
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAtMillis)
            .putString(KEY_USER_EMAIL, session.userEmail)
            .putString(KEY_DEVICE_BINDING_ID, session.deviceBindingId)
            .apply()

        _sessionFlow.value = session
    }

    override suspend fun clearSession() {
        prefs.edit().clear().apply()
        _sessionFlow.value = null
    }

    override suspend fun isSessionValid(): Boolean {
        val session = _sessionFlow.value ?: return false
        return !session.isExpired && session.accessToken.isNotBlank()
    }

    private fun readSessionFromStorage(): AuthSession? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val deviceBindingId = prefs.getString(KEY_DEVICE_BINDING_ID, null)

        return AuthSession(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = expiresAt,
            userEmail = email,
            deviceBindingId = deviceBindingId
        )
    }

    companion object {
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_ACCESS_TOKEN = "session_access_token"
        private const val KEY_REFRESH_TOKEN = "session_refresh_token"
        private const val KEY_EXPIRES_AT = "session_expires_at"
        private const val KEY_USER_EMAIL = "session_user_email"
        private const val KEY_DEVICE_BINDING_ID = "session_device_binding_id"
    }
}

/**
 * In-memory implementation of [SessionRepository] for unit testing.
 */
class InMemorySessionRepository(
    initialSession: AuthSession? = null
) : SessionRepository {

    private val _sessionFlow = MutableStateFlow<AuthSession?>(initialSession)

    override fun observeSession(): Flow<AuthSession?> = _sessionFlow.asStateFlow()

    override suspend fun getSession(): AuthSession? = _sessionFlow.value

    override suspend fun saveSession(session: AuthSession) {
        _sessionFlow.value = session
    }

    override suspend fun clearSession() {
        _sessionFlow.value = null
    }

    override suspend fun isSessionValid(): Boolean {
        val session = _sessionFlow.value ?: return false
        return !session.isExpired && session.accessToken.isNotBlank()
    }
}
