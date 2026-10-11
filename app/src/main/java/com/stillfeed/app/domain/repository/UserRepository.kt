package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for local user identity persistence.
 *
 * Security & Design Rules:
 * - Never returns or stores plaintext passwords.
 * - Decoupled from Room entity classes.
 * - Supports observable reactive flows for UI observation.
 */
interface UserRepository {
    fun observeUser(userId: String): Flow<User?>
    fun observeCurrentUser(): Flow<User?>
    suspend fun getUser(userId: String): User?
    suspend fun getCurrentUser(): User?
    suspend fun saveUser(user: User)
    suspend fun updateUser(user: User)
    suspend fun deleteUser(userId: String)
    suspend fun clearAllUsers()
}
