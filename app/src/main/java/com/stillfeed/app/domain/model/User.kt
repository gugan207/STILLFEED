package com.stillfeed.app.domain.model

/**
 * Domain model representing a persistent local user/account profile in STILLFEED.
 *
 * Security & Architecture rules:
 * - Plaintext passwords are NEVER stored in this model or persisted.
 * - Password hashes are managed exclusively by the authentication provider.
 * - [deviceBindingId] tracks the device-bound restore token for same-device verification.
 */
data class User(
    val id: String,
    val email: String?,
    val displayName: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deviceBindingId: String? = null
)
