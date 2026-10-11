package com.stillfeed.app.domain.model

/**
 * Result of evaluating eligibility for cloud history restoration.
 *
 * Core Product Policy (STEP 05):
 * - SAME DEVICE: Authenticated account + matching device-binding proof -> history restore permitted.
 * - DIFFERENT DEVICE: Authenticated account + non-matching device-binding proof -> history restore denied.
 * - UNKNOWN / INDETERMINATE DEVICE -> history restore denied.
 * - Login alone is NEVER sufficient proof to authorize history restore!
 */
sealed class RestoreDecision {
    /**
     * Authenticated account + matching device-binding proof.
     * History restoration from cloud to local device is authorized.
     */
    data class Permitted(
        val deviceBindingId: String,
        val message: String = "Same device confirmed. History restore authorized."
    ) : RestoreDecision()

    /**
     * Authenticated account + non-matching device-binding proof.
     * History restoration is strictly denied to prevent cross-device migration.
     */
    data class DeniedDifferentDevice(
        val registeredBindingId: String,
        val currentBindingId: String,
        val reason: String = "Account history is bound to a different device. Cross-device restoration denied."
    ) : RestoreDecision()

    /**
     * Platform signal is missing, non-durable, or indeterminate.
     * Restoration is denied rather than claiming a false guarantee.
     */
    data class DeniedIndeterminateDevice(
        val reason: String = "Device platform signal is indeterminate. Restoration cannot be verified."
    ) : RestoreDecision()

    /**
     * Account exists remotely but has no historical device binding or backup recorded.
     */
    data object NoRemoteBackupFound : RestoreDecision()
}

/**
 * Outcome of executing a device-bound restore operation.
 */
sealed class RestoreExecutionResult {
    data class Success(
        val restoredUsageEventsCount: Int,
        val restoredUnlockEventsCount: Int
    ) : RestoreExecutionResult()

    data class Denied(
        val decision: RestoreDecision,
        val reason: String
    ) : RestoreExecutionResult()

    data class Failure(
        val error: Throwable,
        val message: String
    ) : RestoreExecutionResult()
}
