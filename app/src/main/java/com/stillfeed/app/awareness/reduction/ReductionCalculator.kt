package com.stillfeed.app.awareness.reduction

import com.stillfeed.app.domain.model.AwarenessPercentage
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus

/**
 * Pure domain calculator for monthly awareness reduction amounts, target usages, and status (STEP 08 Section 5 & 8).
 *
 * Mathematical Contract:
 * - targetUsage = baselineUsage * (1 - selectedReductionPercentage)
 * - reductionAmount = baselineUsage * selectedReductionPercentage
 *
 * Architectural Guarantees:
 * - Pure integer arithmetic using 64-bit integer intermediates to prevent 32-bit overflow.
 * - Integer half-up rounding: ((product + 50) / 100).
 * - Exact percentage validation (strictly 5%, 10%, 15%, 20%).
 * - Calm, non-judgmental status resolution.
 */
class ReductionCalculator {

    /**
     * Validates that [percentage] is one of the supported values (5, 10, 15, 20).
     *
     * @throws IllegalArgumentException if the percentage is not supported.
     */
    fun validatePercentage(percentage: Int): Int {
        require(AwarenessPercentage.isValid(percentage)) {
            "Unsupported awareness percentage: $percentage%. Supported values are strictly 5%, 10%, 15%, 20%."
        }
        return percentage
    }

    /**
     * Calculates the reduction amount in integer minutes using half-up integer rounding:
     * reduction = round((baselineMinutes * percentage) / 100)
     */
    fun calculateReductionAmount(baselineMinutes: Int, percentage: Int): Int {
        if (baselineMinutes <= 0 || percentage <= 0) return 0
        val clampedPercentage = percentage.coerceIn(0, 100)
        val product = baselineMinutes.toLong() * clampedPercentage
        return ((product + 50) / 100).toInt()
    }

    /**
     * Calculates the target usage allowance in integer minutes:
     * targetUsage = max(0, baselineMinutes - reductionAmount)
     */
    fun calculateTargetUsage(baselineMinutes: Int, percentage: Int): Int {
        if (baselineMinutes <= 0) return 0
        if (percentage <= 0) return baselineMinutes
        val reduction = calculateReductionAmount(baselineMinutes, percentage)
        return (baselineMinutes - reduction).coerceAtLeast(0)
    }

    /**
     * Determines the calm, non-judgmental status of the monthly awareness cycle.
     */
    fun determineStatus(
        hasPermission: Boolean,
        isBaselineSufficient: Boolean,
        baselineMinutes: Int,
        percentage: Int,
        currentProgressMinutes: Int,
        targetUsageMinutes: Int
    ): MonthlyAwarenessStatus {
        if (!hasPermission) {
            return MonthlyAwarenessStatus.DATA_UNAVAILABLE
        }
        if (!isBaselineSufficient || baselineMinutes <= 0 || percentage <= 0) {
            return MonthlyAwarenessStatus.BASELINE_FORMING
        }
        return when {
            targetUsageMinutes > 0 && currentProgressMinutes >= targetUsageMinutes -> {
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
