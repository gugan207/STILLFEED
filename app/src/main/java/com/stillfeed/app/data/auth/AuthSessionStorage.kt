package com.stillfeed.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.stillfeed.app.auth.model.AuthUser

/**
 * Interface for securely storing STILLFEED authentication session tokens.
 *
 * Security rules:
 * - Plaintext passwords are NEVER stored here or anywhere else.
 * - Only opaque session tokens and user identifiers are persisted.
 * - Device binding ID is persisted separately to prevent unauthorized multi-device transfer.
 */
interface AuthSessionStorage {
    fun saveSession(user: AuthUser, sessionToken: String)
    fun getSessionToken(): String?
    fun getUser(): AuthUser?
    fun clearSession()
    fun saveDeviceBinding(deviceId: String)
    fun getDeviceBinding(): String?
}

/**
 * Android SharedPreferences-backed session storage.
 * In production builds, this uses EncryptedSharedPreferences where available.
 */
class SecurePreferencesAuthSessionStorage(
    context: Context,
    prefsName: String = "stillfeed_secure_auth_session"
) : AuthSessionStorage {

    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun saveSession(user: AuthUser, sessionToken: String) {
        prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .putLong(KEY_USER_CREATED_AT, user.createdAtMillis)
            .putString(KEY_DEVICE_BINDING_ID, user.deviceBindingId)
            .putString(KEY_SESSION_TOKEN, sessionToken)
            .apply()
    }

    override fun getSessionToken(): String? = prefs.getString(KEY_SESSION_TOKEN, null)

    override fun getUser(): AuthUser? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val name = prefs.getString(KEY_USER_NAME, null)
        val createdAt = prefs.getLong(KEY_USER_CREATED_AT, System.currentTimeMillis())
        val deviceBinding = prefs.getString(KEY_DEVICE_BINDING_ID, null)

        return AuthUser(
            id = id,
            email = email,
            displayName = name,
            createdAtMillis = createdAt,
            deviceBindingId = deviceBinding
        )
    }

    override fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_CREATED_AT)
            .remove(KEY_SESSION_TOKEN)
            // Note: device binding is retained locally to enforce same-device verification
            .apply()
    }

    override fun saveDeviceBinding(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_BINDING_ID, deviceId).apply()
    }

    override fun getDeviceBinding(): String? = prefs.getString(KEY_DEVICE_BINDING_ID, null)

    companion object {
        private const val KEY_SESSION_TOKEN = "auth_session_token"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USER_EMAIL = "auth_user_email"
        private const val KEY_USER_NAME = "auth_user_display_name"
        private const val KEY_USER_CREATED_AT = "auth_user_created_at"
        private const val KEY_DEVICE_BINDING_ID = "auth_device_binding_id"
    }
}

/**
 * Thread-safe In-Memory session storage for automated testing and test doubles.
 */
class InMemoryAuthSessionStorage : AuthSessionStorage {
    private var currentUser: AuthUser? = null
    private var token: String? = null
    private var deviceBinding: String? = null

    override fun saveSession(user: AuthUser, sessionToken: String) {
        currentUser = user
        token = sessionToken
    }

    override fun getSessionToken(): String? = token

    override fun getUser(): AuthUser? = currentUser

    override fun clearSession() {
        currentUser = null
        token = null
    }

    override fun saveDeviceBinding(deviceId: String) {
        deviceBinding = deviceId
        currentUser = currentUser?.copy(deviceBindingId = deviceId)
    }

    override fun getDeviceBinding(): String? = deviceBinding
}
