package com.stillfeed.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an unlock allowance event.
 */
@Entity(
    tableName = "unlock_events",
    indices = [
        Index(value = ["timestampMillis"]),
        Index(value = ["relatedUsageEventId"])
    ]
)
data class UnlockEventEntity(
    @PrimaryKey
    val id: String,
    val timestampMillis: Long,
    val allowanceMinutes: Int,
    val reason: String,
    val relatedUsageEventId: String?
)
