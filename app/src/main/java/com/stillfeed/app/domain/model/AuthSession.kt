package com.stillfeed.app.domain.model

/**
 * Domain model representing an active authentication session.
 *
 * Security rules:
 * - Plaintext passwords are NEVER stored in this session.
 * - Access and refresh tokens are opaque bearer tokens.
 * - [deviceBindingId] tracks the device-bound restore token for this session.
 */
data class AuthSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtMillis: Long = System.currentTimeMillis() + DEFAULT_TTL_MILLIS,
    val userEmail: String? = null,
    val deviceBindingId: String? = null
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAtMillis

    companion object {
        const val DEFAULT_TTL_MILLIS: Long = 3600 * 1000L // 1 hour default access token TTL
    }
}
