package com.stillfeed.app.feature.awareness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stillfeed.app.awareness.AwarenessManager
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.AwarenessPercentage
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state representing the user's monthly awareness progress and settings (STEP 09 Section 3, 4, 5).
 *
 * PRODUCT CONSTRAINTS:
 * - NEVER score out of 100.
 * - NEVER use shame, guilt, failure, or punishment language.
 * - Integer-safe time representations.
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
    val errorMessage: String? = null
)

/**
 * ViewModel managing the monthly awareness product state (STEP 09).
 */
class AwarenessViewModel(
    private val awarenessManager: AwarenessManager,
    private val usageAggregator: UsageAggregator,
    private val usageStatsDataSource: UsageStatsDataSource? = null,
    private val protectionStateRepository: ProtectionStateRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AwarenessUiState(isLoading = true))
    val uiState: StateFlow<AwarenessUiState> = _uiState.asStateFlow()

    init {
        loadMonthlyState()
        observeProtectionState()
    }

    /**
     * Loads or refreshes the awareness state for the current calendar month.
     */
    fun loadMonthlyState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val currentMonth = awarenessManager.getCurrentMonthIdentifier()
                val hasPermission = usageStatsDataSource?.hasUsageStatsPermission() ?: true

                val monthlyState = awarenessManager.getOrCreateMonthlyState(currentMonth)
                val todaySummary = usageAggregator.getTodayUsage()
                val todayMinutes = (todaySummary.totalMonitoredMillis / 60000L).toInt()

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
            protectionStateRepository: ProtectionStateRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AwarenessViewModel(
                    awarenessManager = awarenessManager,
                    usageAggregator = usageAggregator,
                    usageStatsDataSource = usageStatsDataSource,
                    protectionStateRepository = protectionStateRepository
                ) as T
            }
        }
    }
}
