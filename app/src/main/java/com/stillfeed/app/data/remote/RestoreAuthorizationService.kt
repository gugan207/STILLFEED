package com.stillfeed.app.data.remote

import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent

/**
 * Outcome of enrolling a device with the backend (STEP 05 Remediation 3).
 *
 * Product Rules:
 * 1. First installation (no existing binding):
 *    -> Creates the first active binding and returns [Enrolled] with the single restore credential.
 * 2. Subsequent enrollment attempt / new device when account is already bound:
 *    -> Strictly rejected with [DeniedAlreadyBound].
 *    -> Account login alone CANNOT replace an existing binding or rotate the restore credential.
 */
sealed class DeviceEnrollmentResult {
    data class Enrolled(
        val deviceBindingId: String,
        val restoreCredential: String
    ) : DeviceEnrollmentResult()

    data class DeniedAlreadyBound(
        val reason: String = "Account already has an active device binding. Re-enrollment or binding replacement is denied."
    ) : DeviceEnrollmentResult()
}

/**
 * Result of a server-side restore authorization request.
 *
 * The server performs all authorization verification on its own trusted data:
 * 1. Authenticated user ID derived cryptographically from verified session JWT (auth.uid()).
 * 2. Hash of the server-issued restore credential verified against server-stored enrollment hash.
 * 3. History records strictly isolated to the exact authorized binding.
 *
 * A client CANNOT manufacture authorization by:
 * - Submitting a known device_binding_id.
 * - Changing HTTP headers or query parameters.
 * - Supplying local Room or DataStore values.
 * - Forging device claims.
 */
sealed class RestoreAuthorizationResult {
    /**
     * Server has verified the caller's server-issued restore credential and returned
     * history records strictly belonging to the authorized device binding.
     */
    data class Authorized(
        val authorizedBindingId: String,
        val usageEvents: List<UsageEvent>,
        val unlockEvents: List<UnlockEvent>
    ) : RestoreAuthorizationResult()

    /**
     * Restore denied: The presented credential is invalid, does not match the active
     * enrollment for this account, or represents an unauthorized device.
     * Merely submitting or knowing a device_binding_id results in this rejection.
     */
    data class DeniedUnauthorized(
        val reason: String
    ) : RestoreAuthorizationResult()

    /**
     * Server could not find any active registered device binding for this user account.
     */
    data object NoRegisteredBinding : RestoreAuthorizationResult()

    /**
     * Authorization token or session has expired or been revoked.
     */
    data class Expired(
        val reason: String
    ) : RestoreAuthorizationResult()

    /**
     * Server-side error during authorization.
     */
    data class ServerError(
        val error: Throwable,
        val reason: String
    ) : RestoreAuthorizationResult()
}

/**
 * Server-side RPC contract for device enrollment and device-bound restore operations.
 *
 * SECURITY MODEL (STEP 05 Remediation 3):
 *
 * 1. Single Active Binding & Anti-Replacement Rule:
 *    Account may establish exactly one device binding. If an active binding exists,
 *    any subsequent enrollment attempt returns [DeviceEnrollmentResult.DeniedAlreadyBound].
 *    Ordinary account login or session refresh NEVER replaces the binding or rotates the credential.
 *
 * 2. Purged Client-Claimed Identifiers:
 *    Neither `claimed_device_binding_id`, `x-device-binding-proof`, nor any client-controlled
 *    parameter is accepted as authorization to restore history. A client possessing or knowing
 *    another device's `device_binding_id` cannot restore history.
 *
 * 3. Server-Issued Restore Credential:
 *    Authorization requires a secret, high-entropy credential (`restoreCredential`) issued by
 *    the backend exclusively during initial device enrollment and stored in the device's secure enclave.
 *    The server verifies the SHA-256 hash of this credential against its own database.
 *
 * 4. Strict Query Isolation:
 *    Even when authorized, the server queries history strictly constrained to the authorized binding:
 *    `WHERE user_id = auth.uid() AND device_binding_id = v_authorized_binding`
 *    for BOTH usage_history and unlock_history.
 */
interface RestoreAuthorizationService {
    /**
     * Enrolls the caller's device with the server.
     * Returns [DeviceEnrollmentResult.Enrolled] on first install, or
     * [DeviceEnrollmentResult.DeniedAlreadyBound] if an active binding already exists.
     */
    suspend fun enrollDeviceBinding(
        accessToken: String,
        deviceBindingId: String
    ): Result<DeviceEnrollmentResult>

    /**
     * Requests restore authorization using the server-issued restore credential.
     *
     * @param accessToken The authenticated user's JWT access token.
     * @param restoreCredential The secret, server-issued credential previously granted to this device.
     *                          Submitting a plain device_binding_id will be rejected.
     */
    suspend fun requestRestoreAuthorization(
        accessToken: String,
        restoreCredential: String
    ): RestoreAuthorizationResult
}
