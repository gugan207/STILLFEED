package com.stillfeed.app.data.remote.supabase

import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.repository.RemoteHistoryRepository
import org.json.JSONArray
import org.json.JSONObject

/**
 * Concrete implementation of [RemoteHistoryRepository] syncing history events to Supabase tables.
 */
class SupabaseRemoteHistoryRepository(
    private val client: SupabaseHttpClient
) : RemoteHistoryRepository {

    override suspend fun backupUsageEvents(userId: String, events: List<UsageEvent>): Result<Unit> {
        if (events.isEmpty()) return Result.success(Unit)

        val array = JSONArray()
        events.forEach { event ->
            val obj = JSONObject().apply {
                put("user_id", userId)
                put("device_binding_id", "active_device")
                put("timestamp_ms", event.timestampMillis)
                put("package_name", event.packageName)
                put("surface_category", event.surfaceCategory.name)
                put("event_type", event.eventType.name)
                put("duration_ms", event.durationMillis ?: 0L)
            }
            array.put(obj)
        }

        return client.executeInsert(
            tableName = "usage_history",
            jsonPayload = array.toString()
        )
    }

    override suspend fun backupUnlockEvents(userId: String, events: List<UnlockEvent>): Result<Unit> {
        if (events.isEmpty()) return Result.success(Unit)

        val array = JSONArray()
        events.forEach { event ->
            val obj = JSONObject().apply {
                put("user_id", userId)
                put("device_binding_id", "active_device")
                put("timestamp_ms", event.timestampMillis)
                put("allowance_minutes", event.allowanceMinutes)
                put("reason", event.reason)
            }
            array.put(obj)
        }

        return client.executeInsert(
            tableName = "unlock_history",
            jsonPayload = array.toString()
        )
    }

    override suspend fun deleteRemoteHistory(userId: String): Result<Unit> {
        return Result.success(Unit)
    }
}
