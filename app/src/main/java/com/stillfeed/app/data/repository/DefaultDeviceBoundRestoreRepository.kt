package com.stillfeed.app.data.repository

import com.stillfeed.app.data.local.device.DeviceBindingVerificationResult
import com.stillfeed.app.data.local.device.DeviceRestoreCredentialStore
import com.stillfeed.app.data.local.device.InMemoryDeviceRestoreCredentialStore
import com.stillfeed.app.data.remote.RestoreAuthorizationResult
import com.stillfeed.app.data.remote.RestoreAuthorizationService
import com.stillfeed.app.domain.model.RestoreDecision
import com.stillfeed.app.domain.model.RestoreExecutionResult
import com.stillfeed.app.domain.repository.DeviceBindingRepository
import com.stillfeed.app.domain.repository.DeviceBoundRestoreRepository
import com.stillfeed.app.domain.repository.RemoteUserRepository
import com.stillfeed.app.domain.repository.SessionRepository
import com.stillfeed.app.domain.repository.UnlockHistoryRepository
import com.stillfeed.app.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [DeviceBoundRestoreRepository] enforcing STILLFEED's strict
 * device-bound cloud history restoration rules.
 *
 * STEP 05 Remediation 2 Security Architecture:
 *
 * TWO-LAYER AUTHORIZATION:
 *
 * Layer 1 (Client-Side Pre-Check):
 *   The client verifies the registered device binding token against the current device's
 *   platform signal (e.g. ANDROID_ID) via [DeviceBindingRepository.verifyBinding].
 *   This is a UX optimization and early network rejection gate — it prevents unnecessary server calls
 *   when the device is clearly different. It is NOT the security boundary.
 *
 * Layer 2 (Server-Side Authoritative Decision):
 *   The [RestoreAuthorizationService] submits the authenticated user's JWT and the server-issued
 *   restore credential (established during trusted device enrollment) to the server. The server:
 *   1. Extracts auth.uid() from the verified JWT (cryptographically trusted).
 *   2. Hashes the presented credential and validates it against `device_bindings.restore_credential_hash`.
 *   3. Enforces single active binding cardinality per account.
 *   4. Queries history strictly constrained to the authorized binding:
 *      WHERE user_id = auth.uid() AND device_binding_id = v_authorized_binding
 *   5. Returns history data ONLY if verified.
 *
 * A malicious client CANNOT:
 * - Obtain another device's history merely by knowing or submitting its device-binding ID.
 * - Manufacture authorization by changing headers, parameters, or local Room/DataStore data.
 */
class DefaultDeviceBoundRestoreRepository(
    private val remoteUserRepository: RemoteUserRepository,
    private val restoreAuthorizationService: RestoreAuthorizationService,
    private val deviceBindingRepository: DeviceBindingRepository,
    private val sessionRepository: SessionRepository,
    private val usageRepository: UsageRepository,
    private val unlockHistoryRepository: UnlockHistoryRepository,
    private val restoreCredentialStore: DeviceRestoreCredentialStore = InMemoryDeviceRestoreCredentialStore(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DeviceBoundRestoreRepository {

    override suspend fun evaluateRestoreEligibility(userId: String): RestoreDecision = withContext(ioDispatcher) {
        val registeredBindingResult = remoteUserRepository.getRegisteredDeviceBindingId(userId)
        val registeredBindingId = registeredBindingResult.getOrNull()

        if (registeredBindingId.isNullOrBlank()) {
            return@withContext RestoreDecision.NoRemoteBackupFound
        }

        val currentBinding = deviceBindingRepository.getCurrentBinding()

        when (val verification = deviceBindingRepository.verifyBinding(registeredBindingId)) {
            is DeviceBindingVerificationResult.MatchSameDevice -> {
                RestoreDecision.Permitted(
                    deviceBindingId = registeredBindingId,
                    message = "Same device confirmed via matching device-binding token. History restore authorized."
                )
            }
            is DeviceBindingVerificationResult.MismatchDifferentDevice -> {
                RestoreDecision.DeniedDifferentDevice(
                    registeredBindingId = registeredBindingId,
                    currentBindingId = currentBinding.bindingId,
                    reason = "Account history is bound to a different device (${registeredBindingId.take(12)}...). Automatic cross-device restore is blocked."
                )
            }
            is DeviceBindingVerificationResult.IndeterminatePlatformLimitation -> {
                RestoreDecision.DeniedIndeterminateDevice(
                    reason = "Platform device verification is indeterminate: ${verification.reason}. History restore denied to prevent unverified migration."
                )
            }
        }
    }

    override suspend fun executeDeviceBoundRestore(userId: String): RestoreExecutionResult = withContext(ioDispatcher) {
        // Layer 1: Client-side pre-check (UX & early network gate, NOT the security boundary)
        when (val decision = evaluateRestoreEligibility(userId)) {
            is RestoreDecision.Permitted -> {
                // Layer 2: Server-side authoritative verification (The Security Boundary)
                try {
                    val session = sessionRepository.getSession()
                        ?: return@withContext RestoreExecutionResult.Failure(
                            IllegalStateException("No active session for restore authorization."),
                            "No active session. Please sign in again."
                        )

                    if (session.accessToken.isBlank()) {
                        return@withContext RestoreExecutionResult.Failure(
                            IllegalStateException("Access token is missing."),
                            "Invalid session. Please sign in again."
                        )
                    }

                    // Retrieve the server-issued restore credential from secure local storage
                    val restoreCredential = restoreCredentialStore.getRestoreCredential()
                    if (restoreCredential.isNullOrBlank()) {
                        // Device does not possess the server-issued restore credential!
                        // Even if client knows the binding ID, it cannot restore without the server credential.
                        return@withContext RestoreExecutionResult.Denied(
                            RestoreDecision.DeniedDifferentDevice(
                                registeredBindingId = decision.deviceBindingId,
                                currentBindingId = "missing-credential",
                                reason = "No server-issued restore credential found on this device. Cross-device restore is blocked."
                            ),
                            "No server-issued restore credential found on this device."
                        )
                    }

                    val serverResult = restoreAuthorizationService.requestRestoreAuthorization(
                        accessToken = session.accessToken,
                        restoreCredential = restoreCredential
                    )

                    when (serverResult) {
                        is RestoreAuthorizationResult.Authorized -> {
                            if (serverResult.usageEvents.isNotEmpty()) {
                                usageRepository.recordEvents(serverResult.usageEvents)
                            }
                            serverResult.unlockEvents.forEach { unlockEvent ->
                                unlockHistoryRepository.recordUnlock(unlockEvent)
                            }
                            RestoreExecutionResult.Success(
                                restoredUsageEventsCount = serverResult.usageEvents.size,
                                restoredUnlockEventsCount = serverResult.unlockEvents.size
                            )
                        }
                        is RestoreAuthorizationResult.DeniedUnauthorized -> {
                            RestoreExecutionResult.Denied(
                                RestoreDecision.DeniedDifferentDevice(
                                    registeredBindingId = "server-verified-binding",
                                    currentBindingId = "unauthorized",
                                    reason = serverResult.reason
                                ),
                                serverResult.reason
                            )
                        }
                        is RestoreAuthorizationResult.NoRegisteredBinding -> {
                            RestoreExecutionResult.Denied(
                                RestoreDecision.NoRemoteBackupFound,
                                "Server reports no registered device binding."
                            )
                        }
                        is RestoreAuthorizationResult.Expired -> {
                            RestoreExecutionResult.Failure(
                                IllegalStateException(serverResult.reason),
                                serverResult.reason
                            )
                        }
                        is RestoreAuthorizationResult.ServerError -> {
                            RestoreExecutionResult.Failure(
                                serverResult.error,
                                serverResult.reason
                            )
                        }
                    }
                } catch (e: Exception) {
                    RestoreExecutionResult.Failure(e, e.message ?: "Unexpected error during server-authorized restore")
                }
            }
            is RestoreDecision.DeniedDifferentDevice -> {
                RestoreExecutionResult.Denied(decision, decision.reason)
            }
            is RestoreDecision.DeniedIndeterminateDevice -> {
                RestoreExecutionResult.Denied(decision, decision.reason)
            }
            is RestoreDecision.NoRemoteBackupFound -> {
                RestoreExecutionResult.Denied(decision, "No remote history backup exists for this account.")
            }
        }
    }
}
