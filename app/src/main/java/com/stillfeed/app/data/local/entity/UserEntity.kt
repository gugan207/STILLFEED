package com.stillfeed.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing local user profile data.
 *
 * Security: Plaintext passwords are NEVER stored in this table.
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"])
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String?,
    val displayName: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deviceBindingId: String?
)
