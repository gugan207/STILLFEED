package com.stillfeed.app.data.local.device

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

/**
 * Result of device-binding token verification against the current device.
 */
sealed class DeviceBindingVerificationResult {
    /** The token reliably matches the current device's durable platform binding token. */
    data object MatchSameDevice : DeviceBindingVerificationResult()

    /** The token is from a different physical device (cross-device restore rejected). */
    data object MismatchDifferentDevice : DeviceBindingVerificationResult()

    /**
     * The platform signal cannot guarantee same-device durability across reinstall
     * (e.g. non-durable installation UUID, missing OS identifier, or unsupported environment).
     * The architecture MUST NOT claim a false same-device guarantee.
     */
    data class IndeterminatePlatformLimitation(val reason: String) : DeviceBindingVerificationResult()
}

/**
 * Result of retrieving a device-binding token from a provider.
 */
data class DeviceBindingTokenResult(
    val token: String,
    val isDurableAcrossReinstall: Boolean,
    val mechanismDescription: String
)

/**
 * Abstraction for platform-specific device binding mechanisms.
 *
 * Keeps the persistence and domain layers decoupled from specific Android OS mechanisms,
 * allowing replaceable implementations (Scoped Android ID, Play Services Block Store, etc.)
 * without hard-coding invalid or non-durable identifiers.
 */
interface DeviceBindingProvider {
    val providerName: String
    val isPlatformDurableAcrossReinstall: Boolean

    suspend fun getOrCreateBindingToken(): DeviceBindingTokenResult
    suspend fun verifyBindingToken(registeredToken: String): DeviceBindingVerificationResult
    suspend fun clearBindingToken()
}

/**
 * Device binding provider backed by Android's scoped [Settings.Secure.ANDROID_ID].
 *
 * Android Platform Behavior (API 26+):
 * - On Android 8.0+, ANDROID_ID is scoped to (app signing key + device user + physical device).
 * - Survives app uninstall and reinstall on the SAME device without cloud backup.
 * - Produces a completely distinct value on a DIFFERENT device.
 * - Does NOT survive factory reset.
 * - To preserve privacy, the raw identifier is hashed via SHA-256 with the app package name.
 */
class AndroidIdDeviceBindingProvider(
    private val context: Context
) : DeviceBindingProvider {

    override val providerName: String = "ANDROID_SCOPED_ID"
    override val isPlatformDurableAcrossReinstall: Boolean = true

    override suspend fun getOrCreateBindingToken(): DeviceBindingTokenResult {
        val rawAndroidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        if (rawAndroidId.isNullOrBlank() || rawAndroidId == DUMMY_EMULATOR_ID) {
            return DeviceBindingTokenResult(
                token = "",
                isDurableAcrossReinstall = false,
                mechanismDescription = "ANDROID_ID unavailable or invalid on this runtime environment."
            )
        }
        val hashedToken = hashDeviceIdentifier(context.packageName, rawAndroidId)
        return DeviceBindingTokenResult(
            token = hashedToken,
            isDurableAcrossReinstall = true,
            mechanismDescription = "Scoped SHA-256 derived from Android 8.0+ app-signing-keyed ANDROID_ID."
        )
    }

    override suspend fun verifyBindingToken(registeredToken: String): DeviceBindingVerificationResult {
        if (registeredToken.isBlank()) {
            return DeviceBindingVerificationResult.IndeterminatePlatformLimitation(
                "Registered binding token is blank."
            )
        }
        val currentResult = getOrCreateBindingToken()
        if (!currentResult.isDurableAcrossReinstall || currentResult.token.isBlank()) {
            return DeviceBindingVerificationResult.IndeterminatePlatformLimitation(
                currentResult.mechanismDescription
            )
        }
        return if (currentResult.token == registeredToken) {
            DeviceBindingVerificationResult.MatchSameDevice
        } else {
            DeviceBindingVerificationResult.MismatchDifferentDevice
        }
    }

    override suspend fun clearBindingToken() {
        // ANDROID_ID is OS-managed and cannot be wiped locally; reset is handled at repository level.
    }

    companion object {
        private const val DUMMY_EMULATOR_ID = "9774d56d682e549c"

        fun hashDeviceIdentifier(packageName: String, rawId: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val input = "$packageName:$rawId".toByteArray(Charsets.UTF_8)
            val hashBytes = digest.digest(input)
            return "dev_bind_" + hashBytes.joinToString("") { "%02x".format(it) }.take(24)
        }
    }
}

/**
 * Fallback provider using an app-private UUID stored in SharedPreferences.
 *
 * EXPLICIT PLATFORM LIMITATION:
 * - App-private SharedPreferences are ERADICATED by Android OS on uninstallation.
 * - CANNOT survive uninstall/reinstall.
 * - Explicitly marks [isPlatformDurableAcrossReinstall] as FALSE and refuses to claim a false guarantee.
 */
class InstallationUuidDeviceBindingProvider(
    context: Context,
    prefsName: String = "stillfeed_installation_uuid"
) : DeviceBindingProvider {

    override val providerName: String = "APP_PRIVATE_UUID_FALLBACK"
    override val isPlatformDurableAcrossReinstall: Boolean = false

    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override suspend fun getOrCreateBindingToken(): DeviceBindingTokenResult {
        var existing = prefs.getString(KEY_TOKEN, null)
        if (existing.isNullOrBlank()) {
            existing = "uuid_inst_${UUID.randomUUID()}"
            prefs.edit().putString(KEY_TOKEN, existing).apply()
        }
        return DeviceBindingTokenResult(
            token = existing,
            isDurableAcrossReinstall = false,
            mechanismDescription = "App-private UUID (erased on uninstall; cannot survive reinstall)."
        )
    }

    override suspend fun verifyBindingToken(registeredToken: String): DeviceBindingVerificationResult {
        val currentToken = prefs.getString(KEY_TOKEN, null)
        if (currentToken == null) {
            // Wiped by uninstall: Cannot claim same-device guarantee
            return DeviceBindingVerificationResult.IndeterminatePlatformLimitation(
                "App-private installation UUID was wiped on uninstall. Platform cannot determine same-device status."
            )
        }
        return if (currentToken == registeredToken) {
            DeviceBindingVerificationResult.MatchSameDevice
        } else {
            DeviceBindingVerificationResult.MismatchDifferentDevice
        }
    }

    override suspend fun clearBindingToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val KEY_TOKEN = "installation_uuid_token"
    }
}

/**
 * In-memory test double for simulating device binding behaviors deterministically.
 */
class InMemoryDeviceBindingProvider(
    var currentToken: String? = "test_device_token_alpha",
    override var isPlatformDurableAcrossReinstall: Boolean = true,
    override val providerName: String = "IN_MEMORY_TEST_PROVIDER"
) : DeviceBindingProvider {

    override suspend fun getOrCreateBindingToken(): DeviceBindingTokenResult {
        val token = currentToken ?: "test_device_token_${UUID.randomUUID()}".also { currentToken = it }
        return DeviceBindingTokenResult(
            token = token,
            isDurableAcrossReinstall = isPlatformDurableAcrossReinstall,
            mechanismDescription = "In-memory test double token."
        )
    }

    override suspend fun verifyBindingToken(registeredToken: String): DeviceBindingVerificationResult {
        if (!isPlatformDurableAcrossReinstall) {
            return DeviceBindingVerificationResult.IndeterminatePlatformLimitation(
                "Platform signal is marked non-durable across reinstall."
            )
        }
        val token = currentToken
        if (token == null || token.isBlank()) {
            return DeviceBindingVerificationResult.IndeterminatePlatformLimitation(
                "No active device token found on current runtime."
            )
        }
        return if (token == registeredToken) {
            DeviceBindingVerificationResult.MatchSameDevice
        } else {
            DeviceBindingVerificationResult.MismatchDifferentDevice
        }
    }

    override suspend fun clearBindingToken() {
        currentToken = null
    }
}
