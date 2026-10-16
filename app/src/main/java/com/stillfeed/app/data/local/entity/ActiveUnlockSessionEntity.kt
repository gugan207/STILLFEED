package com.stillfeed.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a durably persisted active batch unlock session (STEP 19).
 *
 * Guarantees:
 * - One active session per package (enforced by unique index on [packageName]).
 * - Persists selected allowance, remaining allowance, expiry, and indeterminate tracking flag.
 * - Allows surviving process death and application recreation.
 */
@Entity(
    tableName = "active_unlock_sessions",
    indices = [
        Index(value = ["package_name"], unique = true)
    ]
)
data class ActiveUnlockSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "selected_batch_allowance")
    val selectedBatchAllowance: Int,

    @ColumnInfo(name = "remaining_batch_allowance")
    val remainingBatchAllowance: Int,

    @ColumnInfo(name = "activity_type")
    val activityType: String,

    @ColumnInfo(name = "verified_activity_completion")
    val verifiedActivityCompletion: Boolean,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long,

    @ColumnInfo(name = "expires_at_millis")
    val expiresAtMillis: Long,

    @ColumnInfo(name = "is_indeterminate_tracking")
    val isIndeterminateTracking: Boolean
)
