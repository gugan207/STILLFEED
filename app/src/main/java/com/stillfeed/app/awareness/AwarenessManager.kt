package com.stillfeed.app.awareness

import com.stillfeed.app.awareness.baseline.BaselineCalculator
import com.stillfeed.app.awareness.reduction.ReductionCalculator
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.domain.repository.AwarenessRepository
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Domain orchestrator for STILLFEED monthly awareness limits, baselines, and progress tracking (STEP 08).
 *
 * Product Rules Enforced:
 * 1. User chooses monthly awareness percentage (strictly 5%, 10%, 15%, or 20%).
 * 2. Selection remains fixed for the specified calendar month.
 * 3. Monthly isolation: A new month creates an isolated record; previous months are immutable.
 * 4. Deterministic baseline formed from at least 7 completed days of usage history.
 * 5. Integer-safe time arithmetic; zero floating-point imprecision.
 * 6. Calm, non-judgmental semantics: no score out of 100, no guilt/shame language.
 */
class AwarenessManager(
    private val awarenessRepository: AwarenessRepository,
    private val usageAggregator: UsageAggregator,
    private val usageStatsDataSource: UsageStatsDataSource? = null,
    private val baselineCalculator: BaselineCalculator = BaselineCalculator(),
    private val reductionCalculator: ReductionCalculator = ReductionCalculator()
) {

    /**
     * Resolves the current UTC month identifier formatted as YYYY-MM (e.g. "2026-10").
     */
    fun getCurrentMonthIdentifier(nowMillis: Long = System.currentTimeMillis()): String {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneOffset.UTC)
        return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    /**
     * Retrieves or initializes the isolated awareness state for [monthIdentifier].
     *
     * If the state does not exist, forms a new baseline from completed historical days
     * prior to this month and persists the initial record.
     */
    suspend fun getOrCreateMonthlyState(
        monthIdentifier: String,
        nowMillis: Long = System.currentTimeMillis()
    ): AwarenessMonthlyState {
        val existing = awarenessRepository.getMonthlyState(monthIdentifier)
        if (existing != null) {
            // If viewing the active month, refresh in-month accumulated progress
            return if (monthIdentifier == getCurrentMonthIdentifier(nowMillis)) {
                refreshMonthlyProgress(monthIdentifier, nowMillis)
            } else {
                existing
            }
        }

        // State does not exist: calculate baseline from history prior to this month
        val hasPermission = usageStatsDataSource?.hasUsageStatsPermission() ?: true
        val yearMonth = try {
            YearMonth.parse(monthIdentifier)
        } catch (_: Exception) {
            YearMonth.now(ZoneOffset.UTC)
        }

        val startOfMonthMillis = yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val lookbackStartMillis = maxOf(0L, startOfMonthMillis - (30L * 24 * 60 * 60 * 1000L))

        val historicalDays = usageAggregator.getHistoricalDailySummaries(lookbackStartMillis, startOfMonthMillis)
        val baselineResult = baselineCalculator.calculateBaseline(historicalDays)

        val currentProgressMinutes = calculateMonthUsageMinutes(monthIdentifier, nowMillis)
        val initialStatus = reductionCalculator.determineStatus(
            hasPermission = hasPermission,
            isBaselineSufficient = baselineResult.isSufficient,
            baselineMinutes = baselineResult.baselineMinutes,
            percentage = 0,
            currentProgressMinutes = currentProgressMinutes,
            targetUsageMinutes = 0
        )

        val newState = AwarenessMonthlyState(
            monthIdentifier = monthIdentifier,
            targetReductionPercentage = 0,
            baselineMinutes = baselineResult.baselineMinutes,
            currentProgressMinutes = currentProgressMinutes,
            targetUsageMinutes = 0,
            reductionAmountMinutes = 0,
            status = initialStatus,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis
        )

        awarenessRepository.saveMonthlyState(newState)
        return newState
    }

    /**
     * Sets the user's fixed awareness percentage for [monthIdentifier].
     *
     * @param percentage Must be strictly 5, 10, 15, or 20.
     * @return Success containing the updated state, or Failure if percentage is invalid.
     */
    suspend fun selectMonthlyAwarenessPercentage(
        monthIdentifier: String,
        percentage: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<AwarenessMonthlyState> {
        return try {
            reductionCalculator.validatePercentage(percentage)

            val currentState = awarenessRepository.getMonthlyState(monthIdentifier)
                ?: getOrCreateMonthlyState(monthIdentifier, nowMillis)

            val hasPermission = usageStatsDataSource?.hasUsageStatsPermission() ?: true
            val reductionMinutes = reductionCalculator.calculateReductionAmount(currentState.baselineMinutes, percentage)
            val targetMinutes = reductionCalculator.calculateTargetUsage(currentState.baselineMinutes, percentage)

            val updatedStatus = reductionCalculator.determineStatus(
                hasPermission = hasPermission,
                isBaselineSufficient = currentState.baselineMinutes > 0,
                baselineMinutes = currentState.baselineMinutes,
                percentage = percentage,
                currentProgressMinutes = currentState.currentProgressMinutes,
                targetUsageMinutes = targetMinutes
            )

            val updatedState = currentState.copy(
                targetReductionPercentage = percentage,
                targetUsageMinutes = targetMinutes,
                reductionAmountMinutes = reductionMinutes,
                status = updatedStatus,
                updatedAtMillis = nowMillis
            )

            awarenessRepository.saveMonthlyState(updatedState)
            Result.success(updatedState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes the accumulated usage minutes for [monthIdentifier] against [usageAggregator].
     */
    suspend fun refreshMonthlyProgress(
        monthIdentifier: String,
        nowMillis: Long = System.currentTimeMillis()
    ): AwarenessMonthlyState {
        val currentState = awarenessRepository.getMonthlyState(monthIdentifier)
            ?: return getOrCreateMonthlyState(monthIdentifier, nowMillis)

        val progressMinutes = calculateMonthUsageMinutes(monthIdentifier, nowMillis)
        val hasPermission = usageStatsDataSource?.hasUsageStatsPermission() ?: true

        val updatedStatus = reductionCalculator.determineStatus(
            hasPermission = hasPermission,
            isBaselineSufficient = currentState.baselineMinutes > 0,
            baselineMinutes = currentState.baselineMinutes,
            percentage = currentState.targetReductionPercentage,
            currentProgressMinutes = progressMinutes,
            targetUsageMinutes = currentState.targetUsageMinutes
        )

        val updatedState = currentState.copy(
            currentProgressMinutes = progressMinutes,
            status = updatedStatus,
            updatedAtMillis = nowMillis
        )

        awarenessRepository.saveMonthlyState(updatedState)
        return updatedState
    }

    /**
     * Calculates the total monitored minutes for [monthIdentifier] up to [nowMillis].
     */
    private suspend fun calculateMonthUsageMinutes(
        monthIdentifier: String,
        nowMillis: Long
    ): Int {
        val yearMonth = try {
            YearMonth.parse(monthIdentifier)
        } catch (_: Exception) {
            YearMonth.now(ZoneOffset.UTC)
        }

        val startOfMonthMillis = yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfMonthMillis = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        val queryEndMillis = minOf(nowMillis, endOfMonthMillis)

        if (startOfMonthMillis >= queryEndMillis) return 0

        val days = usageAggregator.getHistoricalDailySummaries(startOfMonthMillis, queryEndMillis)
        val totalMonitoredMillis = days.sumOf { it.totalMonitoredMillis }
        return (totalMonitoredMillis / 60000L).toInt()
    }
}
