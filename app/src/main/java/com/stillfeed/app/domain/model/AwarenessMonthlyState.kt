package com.stillfeed.app.domain.model

/**
 * Domain model representing persistent awareness state for a calendar month (STEP 08 Section 2).
 *
 * Requirements:
 * - month identifier (e.g., "2026-10")
 * - selected awareness percentage (strictly 5, 10, 15, 20, or 0 if forming)
 * - baseline usage minutes
 * - current monitored usage minutes
 * - target usage minutes
 * - calculated reduction amount minutes
 * - current month status
 * - creation and update timestamps
 *
 * PRODUCT CONSTRAINTS:
 * - Calm, informative, non-judgmental semantics.
 * - Integer-safe time units (minutes).
 * - No scores out of 100, no guilt/shame language.
 */
data class AwarenessMonthlyState(
    /**
     * Stable month identifier formatted as YYYY-MM (e.g., "2026-10").
     */
    val monthIdentifier: String,

    /**
     * Target reduction percentage chosen by the user (5, 10, 15, 20).
     */
    val targetReductionPercentage: Int = 0,

    /**
     * Historical baseline usage in integer minutes.
     */
    val baselineMinutes: Int = 0,

    /**
     * Accumulated recorded usage in integer minutes for this month.
     */
    val currentProgressMinutes: Int = 0,

    /**
     * Calculated target usage in integer minutes for this month.
     */
    val targetUsageMinutes: Int = calculateTargetUsage(baselineMinutes, targetReductionPercentage),

    /**
     * Calculated reduction amount in integer minutes for this month.
     */
    val reductionAmountMinutes: Int = calculateReductionAmount(baselineMinutes, targetReductionPercentage),

    /**
     * Calm, non-judgmental status of the monthly cycle.
     */
    val status: MonthlyAwarenessStatus = calculateDefaultStatus(
        baselineMinutes = baselineMinutes,
        currentProgressMinutes = currentProgressMinutes,
        targetUsageMinutes = targetUsageMinutes,
        percentage = targetReductionPercentage
    ),

    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val selectedReductionPercentage: Int get() = targetReductionPercentage
    val baselineUsageMinutes: Int get() = baselineMinutes
    val currentUsageMinutes: Int get() = currentProgressMinutes
    val targetUsage: Int get() = targetUsageMinutes
    val reductionAmount: Int get() = reductionAmountMinutes

    companion object {
        /**
         * Calculates the reduction amount in integer minutes using half-up integer rounding.
         * reductionAmount = round((baselineMinutes * percentage) / 100)
         */
        fun calculateReductionAmount(baselineMinutes: Int, percentage: Int): Int {
            if (baselineMinutes <= 0 || percentage <= 0) return 0
            val product = baselineMinutes.toLong() * percentage
            return ((product + 50) / 100).toInt()
        }

        /**
         * Calculates target usage: targetUsage = max(0, baselineMinutes - reductionAmount)
         */
        fun calculateTargetUsage(baselineMinutes: Int, percentage: Int): Int {
            if (baselineMinutes <= 0) return 0
            if (percentage <= 0) return baselineMinutes
            val reduction = calculateReductionAmount(baselineMinutes, percentage)
            return (baselineMinutes - reduction).coerceAtLeast(0)
        }

        /**
         * Computes the default status when not explicitly provided.
         */
        fun calculateDefaultStatus(
            baselineMinutes: Int,
            currentProgressMinutes: Int,
            targetUsageMinutes: Int,
            percentage: Int
        ): MonthlyAwarenessStatus {
            if (baselineMinutes <= 0 || percentage <= 0) {
                return MonthlyAwarenessStatus.BASELINE_FORMING
            }
            return when {
                currentProgressMinutes >= targetUsageMinutes && targetUsageMinutes > 0 -> {
                    MonthlyAwarenessStatus.TARGET_REACHED
                }
                currentProgressMinutes < baselineMinutes -> {
                    MonthlyAwarenessStatus.REDUCING
                }
                else -> {
                    MonthlyAwarenessStatus.ON_TRACK
                }
            }
        }
    }
}
