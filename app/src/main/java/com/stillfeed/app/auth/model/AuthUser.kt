package com.stillfeed.app.auth.model

/**
 * Immutable authenticated user model for STILLFEED.
 *
 * Security rules:
 * - Never contains plaintext passwords or authentication tokens.
 * - Represents stable account identity.
 * - Prepared for future same-device restore via [deviceBindingId].
 */
data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val createdAtMillis: Long = System.currentTimeMillis(),
    /**
     * Extension point for the future same-device restore requirement (STEP 03 requirement 12).
     * Account credentials alone must not automatically transfer history to a new device.
     */
    val deviceBindingId: String? = null
) {
    val isDeviceBound: Boolean
        get() = !deviceBindingId.isNullOrBlank()

    fun withDeviceBinding(deviceId: String): AuthUser = copy(deviceBindingId = deviceId)
}
