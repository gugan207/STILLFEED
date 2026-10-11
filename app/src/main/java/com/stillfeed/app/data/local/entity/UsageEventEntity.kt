package com.stillfeed.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an attention or intervention usage event.
 *
 * Privacy Guarantees:
 * - Contains NO media, screenshots, textual messages, or credentials.
 * - Stores solely structural application interaction metadata.
 */
@Entity(
    tableName = "usage_events",
    indices = [
        Index(value = ["timestampMillis"]),
        Index(value = ["packageName"]),
        Index(value = ["packageName", "timestampMillis"])
    ]
)
data class UsageEventEntity(
    @PrimaryKey
    val id: String,
    val timestampMillis: Long,
    val packageName: String,
    val surfaceCategory: String,
    val eventType: String,
    val durationMillis: Long?,
    val isBlocked: Boolean,
    val isUnlocked: Boolean,
    val metadata: String?
)
