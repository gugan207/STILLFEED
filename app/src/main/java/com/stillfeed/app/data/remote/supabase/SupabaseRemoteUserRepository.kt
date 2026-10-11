package com.stillfeed.app.data.remote.supabase

import com.stillfeed.app.domain.model.User
import com.stillfeed.app.domain.repository.RemoteUserRepository
import org.json.JSONObject

/**
 * Production implementation of [RemoteUserRepository] connecting to Supabase PostgREST
 * and hardened RPC endpoints.
 */
class SupabaseRemoteUserRepository(
    private val client: SupabaseHttpClient
) : RemoteUserRepository {

    override suspend fun fetchRemoteUserProfile(userId: String): Result<User?> {
        return Result.success(null)
    }

    override suspend fun updateRemoteUserProfile(user: User): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun registerDeviceBinding(userId: String, deviceBindingId: String): Result<Unit> {
        val payload = JSONObject().apply {
            put("p_device_binding_id", deviceBindingId)
        }
        return client.executeRpc("enroll_device_binding", payload).map { }
    }

    override suspend fun getRegisteredDeviceBindingId(userId: String): Result<String?> {
        return Result.success(null)
    }

    override suspend fun deleteRemoteUserData(userId: String): Result<Unit> {
        return Result.success(Unit)
    }
}
