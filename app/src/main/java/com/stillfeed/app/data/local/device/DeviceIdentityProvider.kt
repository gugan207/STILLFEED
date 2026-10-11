package com.stillfeed.app.data.local.device

import android.content.Context
import java.util.UUID

/**
 * Privacy-preserving Android-appropriate device identity provider.
 *
 * Security & Privacy constraints (STEP 04 Foundation):
 * - NO invasive device fingerprints.
 * - NO IMEI or telephony identifiers.
 * - NO phone numbers.
 * - NO hardware serial numbers or MAC addresses.
 * - Generates an app-scoped, cryptographically random installation UUID stored in private storage.
 * - Resettable upon data deletion.
 */
interface DeviceIdentityProvider {
    fun getOrCreateInstallationId(): String
    fun clearInstallationId()
}

class AppScopedDeviceIdentityProvider(
    context: Context,
    prefsName: String = "stillfeed_device_identity"
) : DeviceIdentityProvider {

    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun getOrCreateInstallationId(): String {
        val existing = prefs.getString(KEY_INSTALLATION_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val generated = "dev_bind_${UUID.randomUUID()}"
        prefs.edit().putString(KEY_INSTALLATION_ID, generated).apply()
        return generated
    }

    override fun clearInstallationId() {
        prefs.edit().remove(KEY_INSTALLATION_ID).apply()
    }

    companion object {
        private const val KEY_INSTALLATION_ID = "app_scoped_installation_id"
    }
}

/**
 * In-memory implementation for isolated unit testing.
 */
class InMemoryDeviceIdentityProvider(
    private var currentId: String? = null
) : DeviceIdentityProvider {
    override fun getOrCreateInstallationId(): String {
        return currentId ?: ("dev_bind_${UUID.randomUUID()}").also { currentId = it }
    }

    override fun clearInstallationId() {
        currentId = null
    }

    fun setInstallationId(id: String) {
        currentId = id
    }
}
