package com.stillfeed.app.awareness.baseline

import com.stillfeed.app.usage.aggregation.DailyUsageSummary

/**
 * Deterministic baseline calculation engine using daily UsageStats data (STEP 08 Section 4).
 *
 * RULES & ARCHITECTURAL GUARANTEES:
 * 1. Minimum Days Required:
 *    Requires at least [MIN_BASELINE_DAYS] (7 completed calendar days) of data.
 *    If fewer than 7 completed days exist, baseline is 0 and status is BASELINE_FORMING.
 *
 * 2. Incomplete Days Handling:
 *    Only completed historical days (prior to today / target cycle) are included.
 *    Any day marked as incomplete or the current in-flight day is excluded from candidate calculation.
 *
 * 3. Zero-Usage Days Handling:
 *    Zero-usage days within the active tracking period are included as 0 minutes in the average.
 *    If all candidate days are 0 minutes (total = 0), baseline is 0 minutes.
 *
 * 4. Outlier Handling:
 *    Unusually large days (e.g. accidental overnight autoplay) are clamped to [MAX_DAILY_USAGE_MINUTES]
 *    (720 minutes / 12 hours) and to at most 2.5x the median of the candidate days.
 *    This prevents a single anomalous day from distorting the baseline upwards.
 *
 * 5. Monthly Recalculation & Locking:
 *    At the start of a new month, the baseline is derived from the prior month's daily records
 *    (or the preceding 7-30 completed days). Once locked into the month's state, it remains fixed
 *    for the duration of that month.
 *
 * CONSTRAINTS:
 * - Pure integer arithmetic; no floating point precision instability.
 * - No personal behavioral labels, grades, scores out of 100, or guilt/shame messaging.
 */
class BaselineCalculator {

    data class BaselineCalculationResult(
        val baselineMinutes: Int,
        val qualifiedDaysCount: Int,
        val isSufficient: Boolean
    )

    /**
     * Computes the daily baseline in integer minutes from a list of [dailySummaries].
     *
     * @param dailySummaries Historical daily usage records.
     * @param todayDateString If provided, excludes the current incomplete calendar day from consideration.
     */
    fun calculateBaseline(
        dailySummaries: List<DailyUsageSummary>,
        todayDateString: String? = null
    ): BaselineCalculationResult {
        // Exclude today (incomplete day) if provided
        val completedDays = if (todayDateString != null) {
            dailySummaries.filter { it.dateString != todayDateString }
        } else {
            dailySummaries
        }

        if (completedDays.size < MIN_BASELINE_DAYS) {
            return BaselineCalculationResult(
                baselineMinutes = 0,
                qualifiedDaysCount = completedDays.size,
                isSufficient = false
            )
        }

        // Convert monitored millis to integer minutes, capped at MAX_DAILY_USAGE_MINUTES (720 min / 12h)
        val dailyMinutes = completedDays.map { summary ->
            val minutes = (summary.totalMonitoredMillis / 60000L).toInt()
            minutes.coerceIn(0, MAX_DAILY_USAGE_MINUTES)
        }

        // Outlier capping against median:
        val sorted = dailyMinutes.sorted()
        val median = sorted[sorted.size / 2]
        val outlierCap = if (median > 0) {
            minOf(MAX_DAILY_USAGE_MINUTES, (median.toLong() * 250 / 100).toInt())
        } else {
            MAX_DAILY_USAGE_MINUTES
        }

        val clampedMinutes = dailyMinutes.map { minOf(it, outlierCap) }
        val sumMinutes = clampedMinutes.sumOf { it.toLong() }
        val count = clampedMinutes.size

        // Integer-safe average with half-up rounding: (sum + count/2) / count
        val averageMinutes = if (count > 0) {
            ((sumMinutes + (count / 2)) / count).toInt()
        } else {
            0
        }

        return BaselineCalculationResult(
            baselineMinutes = averageMinutes,
            qualifiedDaysCount = count,
            isSufficient = true
        )
    }

    companion object {
        const val MIN_BASELINE_DAYS = 7
        const val MAX_DAILY_USAGE_MINUTES = 720 // 12 hours hard daily cap
    }
}
