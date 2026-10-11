package com.stillfeed.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing monthly awareness configuration and tracked usage minutes.
 */
@Entity(tableName = "awareness_monthly_state")
data class AwarenessMonthlyEntity(
    @PrimaryKey
    val monthIdentifier: String,
    val targetReductionPercentage: Int,
    val baselineMinutes: Int,
    val currentProgressMinutes: Int,
    val targetUsageMinutes: Int = 0,
    val reductionAmountMinutes: Int = 0,
    val status: String = "BASELINE_FORMING",
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
