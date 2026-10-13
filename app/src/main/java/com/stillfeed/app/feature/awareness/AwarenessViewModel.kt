package com.stillfeed.app.feature.awareness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stillfeed.app.awareness.AwarenessManager
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.AwarenessPercentage
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.feature.insights.DayRhythmItem
import com.stillfeed.app.usage.aggregation.DailyUsageSummary
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * UI state representing the user's monthly awareness progress and settings (STEP 09 Section 3, 4, 5)
 * as well as Home (Day) and Home (Evening) presentation states (STEP 18).
 *
 * PRODUCT CONSTRAINTS:
 * - NEVER score out of 100.
 * - NEVER use shame, guilt, failure, or punishment language.
 * - Integer-safe time representations.
 * - Zero fabricated/invented watched Reel counts.
 */
data class AwarenessUiState(
    val monthIdentifier: String = "",
    val targetReductionPercentage: Int = 0,
    val baselineMinutes: Int = 0,
    val currentProgressMinutes: Int = 0,
    val targetUsageMinutes: Int = 0,
    val reductionAmountMinutes: Int = 0,
    val status: MonthlyAwarenessStatus = MonthlyAwarenessStatus.BASELINE_FORMING,
    val todayUsageMinutes: Int = 0,
    val isProtectionEnabled: Boolean = true,
    val hasUsageStatsPermission: Boolean = true,
    val isLockedForMonth: Boolean = false,
    val isLoading: Boolean = false,
    val userSelectedPercentage: Int = 0, // In-flight selection before confirmation
    val errorMessage: String? = null,
    // Step 18: Home (Day) & Home (Evening) state
    val isEvening: Boolean = false,
    val dailyReelsAllowance: Int = 30,
    val yesterdayUsageMinutes: Int = 0,
    val yesterdayUsageDifferenceMinutes: Int = 0, // todayMinutes - yesterdayMinutes
    val isReelsCountAvailable: Boolean = false, // Contract: Reel counts are pending dedicated interception tracking
    val watchedReelsToday: Int? = null,
    val weeklyRhythm: List<DayRhythmItem> = emptyList()
)

/**
 * ViewModel managing the monthly awareness product state (STEP 09) and
 * Home dashboard Day/Evening presentation (STEP 18).
 */
class AwarenessViewModel(
    private val awarenessManager: AwarenessManager,
    private val usageAggregator: UsageAggregator,
    private val usageStatsDataSource: UsageStatsDataSource? = null,
    private val protectionStateRepository: ProtectionStateRepository? = null,
    private val appPreferencesRepository: AppPreferencesRepository? = null,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AwarenessUiState(isLoading = true))
    val uiState: StateFlow<AwarenessUiState> = _uiState.asStateFlow()

    init {
        loadMonthlyState()
        observeProtectionState()
        observePreferences()
    }

    /**
     * Loads or refreshes the awareness state for the current calendar month and calculates
     * the Day/Evening state, yesterday comparison, and weekly rhythm for the Home screen.
     */
    fun loadMonthlyState(nowMillis: Long = clock.millis()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val hasPermission = usageStatsDataSource?.hasUsageStatsPermission() ?: true
                val nowZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), clock.zone)
                val todayDate = nowZdt.toLocalDate()
                val localTime = nowZdt.toLocalTime()

                // Day: 06:00 - 17:59, Evening: 18:00 - 05:59
                val isEvening = localTime.hour >= 18 || localTime.hour < 6

                val currentMonth = awarenessManager.getCurrentMonthIdentifier(nowMillis)
                val monthlyState = awarenessManager.getOrCreateMonthlyState(currentMonth, nowMillis)
                val todaySummary = usageAggregator.getTodayUsage(nowMillis)
                val todayMinutes = (todaySummary.totalMonitoredMillis / 60000L).toInt()

                // Calculate yesterday's monitored usage
                val yesterdayDate = todayDate.minusDays(1)
                val startOfYesterdayMillis = yesterdayDate.atStartOfDay(clock.zone).toInstant().toEpochMilli()
                val startOfTodayMillis = todayDate.atStartOfDay(clock.zone).toInstant().toEpochMilli()
                val yesterdaySummaries = usageAggregator.getHistoricalDailySummaries(startOfYesterdayMillis, startOfTodayMillis)
                val yesterdaySummary = yesterdaySummaries.firstOrNull { it.dateString == yesterdayDate.toString() }
                val yesterdayMinutes = ((yesterdaySummary?.totalMonitoredMillis ?: 0L) / 60000L).toInt()
                val yesterdayDifference = todayMinutes - yesterdayMinutes

                // Calculate current week's (Monday to Sunday) rhythm
                val currentMonday = todayDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val startOfWeekMillis = currentMonday.atStartOfDay(clock.zone).toInstant().toEpochMilli()
                val weekSummaries = usageAggregator.getHistoricalDailySummaries(startOfWeekMillis, nowMillis)
                val weeklyRhythm = buildHomeWeeklyRhythm(
                    startMonday = currentMonday,
                    todayDate = todayDate,
                    weekSummaries = weekSummaries,
                    todaySummary = todaySummary
                )

                val persistedAllowance = appPreferencesRepository?.getPreferences()?.dailyReelsAllowance
                    ?: _uiState.value.dailyReelsAllowance

                _uiState.update { current ->
                    current.copy(
                        monthIdentifier = monthlyState.monthIdentifier,
                        targetReductionPercentage = monthlyState.targetReductionPercentage,
                        baselineMinutes = monthlyState.baselineMinutes,
                        currentProgressMinutes = monthlyState.currentProgressMinutes,
                        targetUsageMinutes = monthlyState.targetUsageMinutes,
                        reductionAmountMinutes = monthlyState.reductionAmountMinutes,
                        status = if (!hasPermission) MonthlyAwarenessStatus.DATA_UNAVAILABLE else monthlyState.status,
                        todayUsageMinutes = todayMinutes,
                        hasUsageStatsPermission = hasPermission,
                        isLockedForMonth = monthlyState.targetReductionPercentage > 0,
                        userSelectedPercentage = if (monthlyState.targetReductionPercentage > 0) {
                            monthlyState.targetReductionPercentage
                        } else {
                            current.userSelectedPercentage
                        },
                        isEvening = isEvening,
                        dailyReelsAllowance = persistedAllowance,
                        yesterdayUsageMinutes = yesterdayMinutes,
                        yesterdayUsageDifferenceMinutes = yesterdayDifference,
                        isReelsCountAvailable = false, // Contract: Watched Reel counts pending dedicated interception tracking
                        watchedReelsToday = null,
                        weeklyRhythm = weeklyRhythm,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load awareness state"
                    )
                }
            }
        }
    }

    /**
     * Builds the 7-day Monday through Sunday rhythm for the Home screen.
     */
    private fun buildHomeWeeklyRhythm(
        startMonday: LocalDate,
        todayDate: LocalDate,
        weekSummaries: List<DailyUsageSummary>,
        todaySummary: DailyUsageSummary
    ): List<DayRhythmItem> {
        val summaryByDate = weekSummaries.associateBy { it.dateString }.toMutableMap()
        summaryByDate[todayDate.toString()] = todaySummary

        val days = (0..6).map { offset ->
            val date = startMonday.plusDays(offset.toLong())
            val dateStr = date.toString()
            val summary = summaryByDate[dateStr]
            val mins = ((summary?.totalMonitoredMillis ?: 0L) / 60000L).toInt()
            Triple(date, mins, date == todayDate)
        }

        val maxMins = days.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
        val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

        return days.mapIndexed { index, (date, mins, isToday) ->
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

    private fun observePreferences() {
        val repo = appPreferencesRepository ?: return
        viewModelScope.launch {
            repo.observePreferences().collect { prefs ->
                _uiState.update { current ->
                    current.copy(dailyReelsAllowance = prefs.dailyReelsAllowance)
                }
            }
        }
    }

    /**
     * Sets the user's pending percentage choice (5, 10, 15, or 20) in the selection UI.
     */
    fun onSelectPercentageOption(percentage: Int) {
        if (!AwarenessPercentage.isValid(percentage)) return
        _uiState.update { it.copy(userSelectedPercentage = percentage) }
    }

    /**
     * Confirms and persists the user's awareness choice for the current month.
     *
     * Once confirmed, the percentage is locked for the entire month in accordance with STEP 08.
     */
    fun confirmPercentageSelection(onSuccess: () -> Unit = {}) {
        val selected = _uiState.value.userSelectedPercentage
        if (!AwarenessPercentage.isValid(selected)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentMonth = _uiState.value.monthIdentifier.ifBlank {
                awarenessManager.getCurrentMonthIdentifier()
            }

            val result = awarenessManager.selectMonthlyAwarenessPercentage(currentMonth, selected)
            result.onSuccess { updatedState ->
                _uiState.update { current ->
                    current.copy(
                        targetReductionPercentage = updatedState.targetReductionPercentage,
                        targetUsageMinutes = updatedState.targetUsageMinutes,
                        reductionAmountMinutes = updatedState.reductionAmountMinutes,
                        status = updatedState.status,
                        isLockedForMonth = true,
                        isLoading = false
                    )
                }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Could not lock awareness limit"
                    )
                }
            }
        }
    }

    private fun observeProtectionState() {
        if (protectionStateRepository == null) return
        viewModelScope.launch {
            protectionStateRepository.observeProtectionState().collect { state ->
                _uiState.update { it.copy(isProtectionEnabled = state.isEnabled) }
            }
        }
    }

    companion object {
        /**
         * Formats integer minutes into clean, calm strings (e.g. "45m", "1h 15m").
         */
        fun formatMinutes(minutes: Int): String {
            if (minutes <= 0) return "0m"
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            return when {
                hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
                hours > 0 -> "${hours}h"
                else -> "${remainingMinutes}m"
            }
        }

        fun provideFactory(
            awarenessManager: AwarenessManager,
            usageAggregator: UsageAggregator,
            usageStatsDataSource: UsageStatsDataSource? = null,
            protectionStateRepository: ProtectionStateRepository? = null,
            appPreferencesRepository: AppPreferencesRepository? = null,
            clock: Clock = Clock.systemDefaultZone()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AwarenessViewModel(
                    awarenessManager = awarenessManager,
                    usageAggregator = usageAggregator,
                    usageStatsDataSource = usageStatsDataSource,
                    protectionStateRepository = protectionStateRepository,
                    appPreferencesRepository = appPreferencesRepository,
                    clock = clock
                ) as T
            }
        }
    }
}
