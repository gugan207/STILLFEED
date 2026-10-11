package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.User

/**
 * Domain repository contract for interacting with remote user profile and device-binding registration.
 *
 * Enforces data minimization: only essential account metadata and device-binding token are synced.
 */
interface RemoteUserRepository {
    suspend fun fetchRemoteUserProfile(userId: String): Result<User?>
    suspend fun updateRemoteUserProfile(user: User): Result<Unit>
    suspend fun registerDeviceBinding(userId: String, deviceBindingId: String): Result<Unit>
    suspend fun getRegisteredDeviceBindingId(userId: String): Result<String?>
    suspend fun deleteRemoteUserData(userId: String): Result<Unit>
}
