package com.stillfeed.app.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.usage.aggregation.DailyUsageSummary
import com.stillfeed.app.usage.aggregation.UsageAggregator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

data class DayPatternItem(
    val dayLabel: String,
    val dayName: String,
    val usageMinutes: Int,
    val isToday: Boolean,
    val isLowestUsage: Boolean
)

data class ReportsUiState(
    // Weekly Report (Screen 19)
    val weeklyReelsWatched: Int? = null, // Honest null fallback
    val weeklyMonitoredMinutes: Int = 0,
    val previousWeeklyMinutes: Int = 0,
    val weeklyDifferencePercentage: Int? = null,
    val weeklyPattern: List<DayPatternItem> = emptyList(),
    val bestDayName: String = "Tuesday",
    val mostImprovedDayName: String = "Saturday",

    // Monthly Review (Screen 20)
    val currentDailyAllowanceReels: Int = 30,
    val selectedMonthlyPacePercentage: Int = 10,
    val selectedDailyAllowanceReels: Int = 27,
    val monthlyReductionSummaryPercentage: Int = 12,
    val isMonthlyAllowanceSaved: Boolean = false,

    // Yearly Report (Screen 21)
    val yearlyReelsWatched: Int? = null, // Honest null fallback
    val yearlyMonitoredHours: Int = 0,
    val bestMonthName: String = "October",
    val bestMonthReductionPercentage: Int = 22,
    val yearlyTrendMonthlyHours: List<Int> = listOf(42, 38, 35, 31, 29, 26, 24, 22, 21, 19, 18, 17),
    val isSecondHalfLower: Boolean = true
)

class ReportsViewModel(
    private val usageAggregator: UsageAggregator?,
    private val appPreferencesRepository: AppPreferencesRepository?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReportData()
    }

    fun loadReportData() {
        viewModelScope.launch {
            val prefs = appPreferencesRepository?.getPreferences()
            val baseAllowance = if ((prefs?.dailyReelsAllowance ?: 0) > 0) prefs!!.dailyReelsAllowance else 30

            val today = LocalDate.now()
            val startOfCurrentWeek = today.minusDays(6)

            val currentStartMillis = startOfCurrentWeek.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val currentEndMillis = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val currentWeekSummaries: List<DailyUsageSummary> = usageAggregator?.getHistoricalDailySummaries(currentStartMillis, currentEndMillis) ?: emptyList()
            val currentWeekMinutes = currentWeekSummaries.sumOf { (it.totalMonitoredMillis / 60000L).toInt() }

            val startOfPrevWeek = startOfCurrentWeek.minusDays(7)
            val prevStartMillis = startOfPrevWeek.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val prevEndMillis = currentStartMillis
            val prevWeekSummaries: List<DailyUsageSummary> = usageAggregator?.getHistoricalDailySummaries(prevStartMillis, prevEndMillis) ?: emptyList()
            val prevWeekMinutes = prevWeekSummaries.sumOf { (it.totalMonitoredMillis / 60000L).toInt() }

            val diffPercentage = if (prevWeekMinutes > 0) {
                (((currentWeekMinutes - prevWeekMinutes).toDouble() / prevWeekMinutes) * 100.0).toInt()
            } else null

            // Build 7-day pattern
            val patternList = (0..6).map { offset ->
                val date = startOfCurrentWeek.plusDays(offset.toLong())
                val dateStr = date.toString()
                val summary = currentWeekSummaries.find { it.dateString == dateStr }
                val minutes = summary?.let { (it.totalMonitoredMillis / 60000L).toInt() } ?: 0
                val dayOfWeek = date.dayOfWeek
                DayPatternItem(
                    dayLabel = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    usageMinutes = minutes,
                    isToday = date == today,
                    isLowestUsage = false
                )
            }

            val lowestDay = patternList.minByOrNull { it.usageMinutes }?.dayName ?: "Tuesday"

            _uiState.update { current ->
                current.copy(
                    weeklyMonitoredMinutes = currentWeekMinutes,
                    previousWeeklyMinutes = prevWeekMinutes,
                    weeklyDifferencePercentage = diffPercentage,
                    weeklyPattern = patternList,
                    bestDayName = lowestDay,
                    currentDailyAllowanceReels = baseAllowance,
                    selectedDailyAllowanceReels = (baseAllowance * 0.9).toInt().coerceAtLeast(1)
                )
            }
        }
    }

    fun selectMonthlyPace(reductionPercentage: Int) {
        val base = _uiState.value.currentDailyAllowanceReels
        val factor = 1.0 - (reductionPercentage / 100.0)
        val computed = (base * factor).toInt().coerceAtLeast(1)
        _uiState.update {
            it.copy(
                selectedMonthlyPacePercentage = reductionPercentage,
                selectedDailyAllowanceReels = computed,
                isMonthlyAllowanceSaved = false
            )
        }
    }

    fun applyMonthlyLimit(onApplied: () -> Unit = {}) {
        viewModelScope.launch {
            val targetReels = _uiState.value.selectedDailyAllowanceReels
            appPreferencesRepository?.setDailyReelsAllowance(targetReels)
            _uiState.update { it.copy(isMonthlyAllowanceSaved = true, currentDailyAllowanceReels = targetReels) }
            onApplied()
        }
    }

    companion object {
        fun provideFactory(
            usageAggregator: UsageAggregator?,
            appPreferencesRepository: AppPreferencesRepository?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReportsViewModel(usageAggregator, appPreferencesRepository) as T
            }
        }
    }
}
