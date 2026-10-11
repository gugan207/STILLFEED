package com.stillfeed.app.data.remote.supabase

import com.stillfeed.app.data.remote.DeviceEnrollmentResult
import com.stillfeed.app.data.remote.RestoreAuthorizationResult
import com.stillfeed.app.data.remote.RestoreAuthorizationService
import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent
import org.json.JSONArray
import org.json.JSONObject

/**
 * Concrete implementation of [RestoreAuthorizationService] invoking hardened
 * PostgreSQL SECURITY DEFINER RPCs on the verified Supabase project.
 */
class SupabaseRestoreAuthorizationService(
    private val client: SupabaseHttpClient
) : RestoreAuthorizationService {

    override suspend fun enrollDeviceBinding(
        accessToken: String,
        deviceBindingId: String
    ): Result<DeviceEnrollmentResult> {
        if (accessToken.isBlank()) {
            return Result.failure(SecurityException("Unauthenticated caller"))
        }

        val params = JSONObject().apply {
            put("p_device_binding_id", deviceBindingId)
        }

        val result = client.executeRpc("enroll_device_binding", params, accessToken = accessToken)

        return result.mapCatching { jsonStr ->
            val json = JSONObject(jsonStr)
            val status = json.optString("status")

            when (status) {
                "ENROLLED" -> {
                    DeviceEnrollmentResult.Enrolled(
                        deviceBindingId = json.getString("device_binding_id"),
                        restoreCredential = json.getString("restore_credential")
                    )
                }
                "DENIED_ALREADY_BOUND" -> {
                    DeviceEnrollmentResult.DeniedAlreadyBound(
                        reason = json.optString("message", "Account already has an active device binding.")
                    )
                }
                else -> {
                    throw IllegalStateException("Unexpected enrollment response status: $status")
                }
            }
        }.recoverCatching { error ->
            if (error is SupabaseApiException && error.statusCode == 401) {
                throw SecurityException("Unauthenticated caller: ${error.message}")
            }
            throw error
        }
    }

    override suspend fun requestRestoreAuthorization(
        accessToken: String,
        restoreCredential: String
    ): RestoreAuthorizationResult {
        if (accessToken.isBlank()) {
            return RestoreAuthorizationResult.ServerError(
                SecurityException("Unauthenticated caller"),
                "Unauthenticated caller"
            )
        }

        val params = JSONObject().apply {
            put("p_restore_credential", restoreCredential)
        }

        val result = client.executeRpc("authorize_device_restore", params, accessToken = accessToken)

        return result.fold(
            onSuccess = { jsonStr ->
                try {
                    val json = JSONObject(jsonStr)
                    val status = json.optString("status")

                    when (status) {
                        "AUTHORIZED" -> {
                            val bindingId = json.getString("authorized_binding_id")
                            val usageArray = json.optJSONArray("usage_events") ?: JSONArray()
                            val unlockArray = json.optJSONArray("unlock_events") ?: JSONArray()

                            val usageEvents = mutableListOf<UsageEvent>()
                            for (i in 0 until usageArray.length()) {
                                val item = usageArray.getJSONObject(i)
                                val surfaceStr = item.optString("surface_category", "UNKNOWN")
                                val category = try {
                                    com.stillfeed.app.domain.model.SurfaceCategory.valueOf(surfaceStr)
                                } catch (_: Exception) {
                                    com.stillfeed.app.domain.model.SurfaceCategory.UNKNOWN
                                }

                                val eventTypeStr = item.optString("event_type", "SESSION_START")
                                val type = try {
                                    com.stillfeed.app.domain.model.UsageEventType.valueOf(eventTypeStr)
                                } catch (_: Exception) {
                                    com.stillfeed.app.domain.model.UsageEventType.SESSION_START
                                }

                                usageEvents.add(
                                    UsageEvent(
                                        id = item.optString("id"),
                                        timestampMillis = item.optLong("timestamp_ms"),
                                        packageName = item.optString("package_name"),
                                        surfaceCategory = category,
                                        eventType = type,
                                        durationMillis = item.optLong("duration_ms"),
                                        isBlocked = false,
                                        isUnlocked = false
                                    )
                                )
                            }

                            val unlockEvents = mutableListOf<UnlockEvent>()
                            for (i in 0 until unlockArray.length()) {
                                val item = unlockArray.getJSONObject(i)
                                unlockEvents.add(
                                    UnlockEvent(
                                        id = item.optString("id"),
                                        timestampMillis = item.optLong("timestamp_ms"),
                                        allowanceMinutes = item.optInt("allowance_minutes"),
                                        reason = item.optString("reason")
                                    )
                                )
                            }

                            RestoreAuthorizationResult.Authorized(
                                authorizedBindingId = bindingId,
                                usageEvents = usageEvents,
                                unlockEvents = unlockEvents
                            )
                        }
                        "DENIED_UNAUTHORIZED" -> {
                            RestoreAuthorizationResult.DeniedUnauthorized(
                                reason = json.optString("message", "Restore denied: Invalid device restore credential.")
                            )
                        }
                        "DENIED_INVALID_CREDENTIAL" -> {
                            RestoreAuthorizationResult.DeniedUnauthorized(
                                reason = json.optString("message", "A valid server-issued restore credential is required.")
                            )
                        }
                        "UNAUTHENTICATED" -> {
                            RestoreAuthorizationResult.ServerError(
                                SecurityException(json.optString("message")),
                                "Caller is not authenticated."
                            )
                        }
                        else -> {
                            RestoreAuthorizationResult.ServerError(
                                IllegalStateException("Unexpected status: $status"),
                                "Server returned unknown status: $status"
                            )
                        }
                    }
                } catch (e: Exception) {
                    RestoreAuthorizationResult.ServerError(e, "Failed to parse restore response: ${e.message}")
                }
            },
            onFailure = { error ->
                if (error is SupabaseApiException && error.statusCode == 401) {
                    RestoreAuthorizationResult.ServerError(
                        SecurityException("Supabase authentication failed"),
                        error.message
                    )
                } else {
                    RestoreAuthorizationResult.ServerError(error, error.message ?: "Unknown error")
                }
            }
        )
    }
}
