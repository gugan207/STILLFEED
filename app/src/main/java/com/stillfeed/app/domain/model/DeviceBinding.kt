package com.stillfeed.app.domain.model

/**
 * Status of device binding for the current installation.
 */
enum class DeviceBindingStatus {
    /** No device binding has been registered yet. */
    UNBOUND,
    /** Successfully bound to this installation. */
    BOUND,
    /** Verified as matching this same physical device across install/reinstall. */
    VERIFIED_SAME_DEVICE,
    /** Token belongs to a different device; cross-device restore rejected. */
    MISMATCH_DIFFERENT_DEVICE,
    /**
     * Platform signal is missing, non-durable, or ambiguous.
     * The architecture MUST NOT claim a false same-device guarantee.
     */
    INDETERMINATE
}

/**
 * Domain model representing local device binding metadata.
 *
 * Product Requirement (STEP 04 Remediation):
 * - History may restore after reinstall on the SAME device.
 * - History must NOT automatically restore on a DIFFERENT phone.
 * - An app-private installation UUID does NOT survive uninstall/reinstall.
 * - Does not claim stronger guarantees than the underlying platform mechanism supports.
 *
 * UNRESOLVED ARCHITECTURAL REQUIREMENT:
 * "Remote/sync restoration is deferred until the production authentication/cloud architecture is selected."
 */
data class DeviceBinding(
    val bindingId: String,
    val status: DeviceBindingStatus = DeviceBindingStatus.BOUND,
    val isDurableAcrossReinstall: Boolean = false,
    val providerName: String = "UNKNOWN",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastVerifiedAtMillis: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0"
)
