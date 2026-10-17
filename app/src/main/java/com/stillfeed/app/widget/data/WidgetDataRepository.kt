package com.stillfeed.app.widget.data

import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.protection.permission.AccessibilityPermissionChecker
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.widget.model.WidgetTimeTheme
import com.stillfeed.app.widget.model.WidgetUiState
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalTime
import java.time.ZoneId

/**
 * Repository providing aggregated, honest local state for Android Home Screen Widgets.
 *
 * Privacy & Data Integrity Principles:
 * - Reads directly from local Room entities and DataStore preferences.
 * - Works completely offline.
 * - Never fabricates Reel watch counts or estimates Reel numbers from minutes.
 * - Preserves calm, encouraging Figma editorial messages.
 */
class WidgetDataRepository(
    private val usageAggregator: UsageAggregator,
    private val protectionStateRepository: ProtectionStateRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val accessibilityPermissionChecker: AccessibilityPermissionChecker
) {
    /**
     * Resolves the current widget UI state at the specified time and epoch millis.
     */
    suspend fun getWidgetUiState(
        currentTime: LocalTime = LocalTime.now(ZoneId.systemDefault()),
        nowMillis: Long = System.currentTimeMillis()
    ): WidgetUiState {
        // 1. Resolve diurnal theme based on local time-of-day
        val theme = resolveTheme(currentTime)

        // 2. Fetch local verified usage totals
        val todaySummary = runCatching {
            usageAggregator.getTodayUsage(nowMillis)
        }.getOrNull()

        val monitoredMinutes = ((todaySummary?.totalMonitoredMillis ?: 0L) / 60_000L).coerceAtLeast(0L)
        val blockedInterventions = todaySummary?.blockedInterventionCount ?: 0

        // 3. Fetch protection state & accessibility permission status
        val protectionState = runCatching {
            protectionStateRepository.getProtectionState()
        }.getOrNull()
        val isProtectionEnabled = protectionState?.isEnabled ?: true
        val isAccessibilityActive = accessibilityPermissionChecker.isAccessibilityPermissionGranted()

        // 4. Fetch daily allowance configuration
        val preferences = runCatching {
            appPreferencesRepository.getPreferences()
        }.getOrNull()
        val allowanceMinutes = preferences?.dailyReelsAllowance ?: 30

        // 5. Select calm, editorial messaging matching Figma
        val message = when {
            !isAccessibilityActive || !isProtectionEnabled -> "Protection paused · Tap to resume"
            theme == WidgetTimeTheme.MORNING -> "Your mind looks fresh."
            theme == WidgetTimeTheme.DAY -> "Nice and slow. You’re doing well."
            theme == WidgetTimeTheme.AFTERNOON -> "You’ve been here a while."
            theme == WidgetTimeTheme.EVENING -> "Take a breath. You can still reset."
            theme == WidgetTimeTheme.NIGHT -> "Rest well. You’ve done enough."
            else -> "Your mind looks fresh."
        }

        // USAGE DATA HONESTY:
        // Reel count is null when unmeasured, causing the widget to display em-dash ("—").
        // We never fabricate reel numbers or derive them from duration.
        val reelCount: Int? = null

        return WidgetUiState(
            theme = theme,
            reelCount = reelCount,
            monitoredMinutes = monitoredMinutes,
            dailyAllowanceMinutes = allowanceMinutes,
            blockedInterventions = blockedInterventions,
            isProtectionActive = isProtectionEnabled,
            isAccessibilityGranted = isAccessibilityActive,
            editorialMessage = message,
            editorialHeadline = "Your attention is still yours.",
            isOffline = false
        )
    }

    companion object {
        fun resolveTheme(time: LocalTime): WidgetTimeTheme {
            return when (time.hour) {
                in 6..11 -> WidgetTimeTheme.MORNING
                in 12..15 -> WidgetTimeTheme.DAY
                in 16..18 -> WidgetTimeTheme.AFTERNOON
                in 19..21 -> WidgetTimeTheme.EVENING
                else -> WidgetTimeTheme.NIGHT
            }
        }
    }
}
