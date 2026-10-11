package com.stillfeed.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stillfeed.app.awareness.AwarenessManager
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.usage.aggregation.DailyUsageSummary
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Single day entry in the 7-day Daily Rhythm visualization (Figma Node 70:125-70:138).
 */
data class DayRhythmItem(
    val dayLabel: String,         // "M", "T", "W", "T", "F", "S", "S"
    val fullDateString: String,   // "YYYY-MM-DD"
    val usageMinutes: Int,
    val normalizedHeightFraction: Float, // 0.0f .. 1.0f
    val isToday: Boolean = false
)

/**
 * UI state for STILLFEED Insights screen (STEP 10 Section 2, 3, 4, 5, 6).
 *
 * PRODUCT CONSTRAINTS:
 * - Strictly zero scores out of 100.
 * - Strictly zero shame, guilt, failure, or "lazy" messaging.
 * - Local-first, private historical metrics only.
 */
data class InsightsUiState(
    val isLoading: Boolean = false,
    val isFirstDayOrEmpty: Boolean = false,
    val hasUsageStatsPermission: Boolean = true,
    val weeklyMonitoredMinutes: Int = 0,
    val previousWeeklyMinutes: Int = 0,
    val weeklyDifferenceMinutes: Int = 0, // negative means reduced (e.g. -18m)
    val weeklyDifferencePercentage: Int? = null,
    val instagramWeeklyMinutes: Int = 0,
    val youtubeWeeklyMinutes: Int = 0,
    val blockedInterventionCount: Int = 0,
    val dailyRhythm: List<DayRhythmItem> = emptyList(),
    val todayMinutes: Int = 0,
    val monthlyAwarenessState: AwarenessMonthlyState? = null,
    val reflectionHeadline: String = "Your lowest-use days are getting easier.",
    val reflectionDetail: String = "Keep the small resets. They are adding up.",
    val errorMessage: String? = null
)

/**
 * ViewModel orchestrating historical usage analytics and calm reflection insights.
 */
class InsightsViewModel(
    private val usageAggregator: UsageAggregator,
    private val awarenessManager: AwarenessManager,
    private val usageStatsDataSource: UsageStatsDataSource? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState(isLoading = true))
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    /**
     * Loads historical weekly usage, daily rhythm distribution, and monthly awareness state.
     */
    fun loadInsights(nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val hasPermission = usageStatsDataSource?.hasUsageStatsPermission() ?: true
                val nowZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneOffset.UTC)
                val todayDate = nowZdt.toLocalDate()

                // Calculate the current 7-day window [startOfCurrentWeekMillis .. nowMillis]
                // 7 days ending today (inclusive)
                val sevenDaysAgoDate = todayDate.minusDays(6)
                val startOfCurrentWeekMillis = sevenDaysAgoDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

                // Prior 7-day comparison window [startOfPriorWeekMillis .. startOfCurrentWeekMillis]
                val fourteenDaysAgoDate = todayDate.minusDays(13)
                val startOfPriorWeekMillis = fourteenDaysAgoDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

                // 1. Fetch current 7 days
                val currentWeekDays = usageAggregator.getHistoricalDailySummaries(
                    startMillis = startOfCurrentWeekMillis,
                    endMillis = nowMillis
                )

                // 2. Fetch prior 7 days for comparison
                val priorWeekDays = usageAggregator.getHistoricalDailySummaries(
                    startMillis = startOfPriorWeekMillis,
                    endMillis = startOfCurrentWeekMillis
                )

                // Today usage
                val todaySummary = usageAggregator.getTodayUsage(nowMillis)
                val todayMins = (todaySummary.totalMonitoredMillis / 60000L).toInt()

                // Monthly awareness integration
                val currentMonthId = awarenessManager.getCurrentMonthIdentifier(nowMillis)
                val awarenessState = awarenessManager.getOrCreateMonthlyState(currentMonthId, nowMillis)

                // Weekly aggregations
                val currentTotalMillis = currentWeekDays.sumOf { it.totalMonitoredMillis }
                val currentInstagramMillis = currentWeekDays.sumOf { it.instagramUsageMillis }
                val currentYoutubeMillis = currentWeekDays.sumOf { it.youtubeUsageMillis }
                val currentBlockedCount = currentWeekDays.sumOf { it.blockedInterventionCount } + todaySummary.blockedInterventionCount

                val currentWeeklyMins = (currentTotalMillis / 60000L).toInt()
                val priorTotalMillis = priorWeekDays.sumOf { it.totalMonitoredMillis }
                val priorWeeklyMins = (priorTotalMillis / 60000L).toInt()

                val diffMins = currentWeeklyMins - priorWeeklyMins
                val diffPercent = if (priorWeeklyMins > 0) {
                    ((diffMins.toLong() * 100L) / priorWeeklyMins).toInt()
                } else null

                // Build 7-day rhythm items (Monday through Sunday or last 7 days)
                val rhythmItems = buildDailyRhythm(
                    startDate = sevenDaysAgoDate,
                    todayDate = todayDate,
                    summaries = currentWeekDays,
                    todaySummary = todaySummary
                )

                // Check for first day or empty state:
                // If permission is missing, or total history is 0 and today is 0
                val isEmpty = !hasPermission || (currentWeeklyMins == 0 && todayMins == 0 && currentWeekDays.isEmpty())

                // Calm reflection text derived from actual progress without shame
                val (headline, detail) = deriveReflectionCopy(
                    awarenessState = awarenessState,
                    diffMins = diffMins,
                    currentWeeklyMins = currentWeeklyMins
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFirstDayOrEmpty = isEmpty,
                        hasUsageStatsPermission = hasPermission,
                        weeklyMonitoredMinutes = currentWeeklyMins,
                        previousWeeklyMinutes = priorWeeklyMins,
                        weeklyDifferenceMinutes = diffMins,
                        weeklyDifferencePercentage = diffPercent,
                        instagramWeeklyMinutes = (currentInstagramMillis / 60000L).toInt(),
                        youtubeWeeklyMinutes = (currentYoutubeMillis / 60000L).toInt(),
                        blockedInterventionCount = currentBlockedCount,
                        dailyRhythm = rhythmItems,
                        todayMinutes = todayMins,
                        monthlyAwarenessState = awarenessState,
                        reflectionHeadline = headline,
                        reflectionDetail = detail
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load insights"
                    )
                }
            }
        }
    }

    private fun buildDailyRhythm(
        startDate: LocalDate,
        todayDate: LocalDate,
        summaries: List<DailyUsageSummary>,
        todaySummary: DailyUsageSummary
    ): List<DayRhythmItem> {
        val summaryByDate = summaries.associateBy { it.dateString }.toMutableMap()
        summaryByDate[todayDate.toString()] = todaySummary

        val dayList = (0..6).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val dateStr = date.toString()
            val summary = summaryByDate[dateStr]
            val mins = ((summary?.totalMonitoredMillis ?: 0L) / 60000L).toInt()
            Triple(date, mins, date == todayDate)
        }

        val maxMins = dayList.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

        val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

        return dayList.mapIndexed { index, (date, mins, isToday) ->
            // Use day of week letter (Monday = M, etc.)
            val label = when (date.dayOfWeek.value) {
                1 -> "M"
                2 -> "T"
                3 -> "W"
                4 -> "T"
                5 -> "F"
                6 -> "S"
                7 -> "S"
                else -> dayLetters.getOrElse(index) { "M" }
            }

            val fraction = (mins.toFloat() / maxMins.toFloat()).coerceIn(0.08f, 1.0f)

            DayRhythmItem(
                dayLabel = label,
                fullDateString = date.toString(),
                usageMinutes = mins,
                normalizedHeightFraction = if (mins == 0) 0.05f else fraction,
                isToday = isToday
            )
        }
    }

    private fun deriveReflectionCopy(
        awarenessState: AwarenessMonthlyState?,
        diffMins: Int,
        currentWeeklyMins: Int
    ): Pair<String, String> {
        return when {
            awarenessState?.status == MonthlyAwarenessStatus.TARGET_REACHED -> {
                Pair(
                    "Monthly target accomplished.",
                    "You stayed within your intended reduction for this month."
                )
            }
            diffMins < 0 -> {
                val absDiff = -diffMins
                Pair(
                    "Your lowest-use days are getting easier.",
                    "Down ${AwarenessViewModel.formatMinutes(absDiff)} from last week. Keep the small resets."
                )
            }
            awarenessState == null || awarenessState.status == MonthlyAwarenessStatus.BASELINE_FORMING -> {
                Pair(
                    "Establishing your reference rhythm.",
                    "Your first week creates a steady baseline without judgment."
                )
            }
            diffMins == 0 -> {
                Pair(
                    "Steady usage rhythm.",
                    "Your screen time remained consistent across the week."
                )
            }
            else -> {
                Pair(
                    "A quiet space to notice your time.",
                    "Awareness begins with simple, clear observation."
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            usageAggregator: UsageAggregator,
            awarenessManager: AwarenessManager,
            usageStatsDataSource: UsageStatsDataSource? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InsightsViewModel(
                    usageAggregator = usageAggregator,
                    awarenessManager = awarenessManager,
                    usageStatsDataSource = usageStatsDataSource
                ) as T
            }
        }
    }
}
