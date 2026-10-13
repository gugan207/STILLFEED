package com.stillfeed.app.feature.edgestates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stillfeed.app.feature.edgestates.connectivity.NetworkConnectivityObserver
import com.stillfeed.app.feature.edgestates.model.BatchFinishedUiState
import com.stillfeed.app.feature.edgestates.model.FirstDayUiState
import com.stillfeed.app.feature.edgestates.model.OfflineUiState
import com.stillfeed.app.feature.edgestates.model.PermissionDisabledUiState
import com.stillfeed.app.protection.permission.AccessibilityPermissionChecker
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Screen 15 — Daily Limit / Batch Finished (Figma Node 70:198).
 *
 * Designed with a clean contract for Step 19 integration.
 */
class BatchFinishedViewModel(
    private val usageAggregator: UsageAggregator? = null,
    private val initialDailyAllowanceReels: Int = 30,
    initialBatchReels: Int? = null,
    initialBatchDurationMinutes: Int? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BatchFinishedUiState(
            dailyAllowanceReels = initialDailyAllowanceReels,
            batchUnlockedReelsCount = initialBatchReels,
            batchDurationMinutes = initialBatchDurationMinutes
        )
    )
    val uiState: StateFlow<BatchFinishedUiState> = _uiState.asStateFlow()

    init {
        loadBatchFinishedState()
    }

    fun loadBatchFinishedState(nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val todayMins = usageAggregator?.let {
                    val summary = it.getTodayUsage(nowMillis)
                    (summary.totalMonitoredMillis / 60000L).toInt()
                } ?: 0

                _uiState.update { current ->
                    current.copy(
                        todayMonitoredMinutes = todayMins,
                        dailyAllowanceReels = initialDailyAllowanceReels,
                        isReelsCountAvailable = false, // Contract: Reel counts pending dedicated tracking
                        todayReelsWatched = null,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to refresh batch metrics"
                    )
                }
            }
        }
    }

    /**
     * Step 19 integration hook: updates the expired batch details when received.
     */
    fun updateExpiredBatch(unlockedReels: Int?, durationMinutes: Int?) {
        _uiState.update { current ->
            current.copy(
                batchUnlockedReelsCount = unlockedReels,
                batchDurationMinutes = durationMinutes
            )
        }
    }

    companion object {
        fun provideFactory(
            usageAggregator: UsageAggregator? = null,
            initialDailyAllowanceReels: Int = 30,
            initialBatchReels: Int? = null,
            initialBatchDurationMinutes: Int? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BatchFinishedViewModel(
                    usageAggregator = usageAggregator,
                    initialDailyAllowanceReels = initialDailyAllowanceReels,
                    initialBatchReels = initialBatchReels,
                    initialBatchDurationMinutes = initialBatchDurationMinutes
                ) as T
            }
        }
    }
}

/**
 * ViewModel for Screen 16 — Permission Disabled (Figma Node 70:221).
 */
class PermissionDisabledViewModel(
    private val accessibilityPermissionChecker: AccessibilityPermissionChecker,
    private val usageStatsDataSource: UsageStatsDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionDisabledUiState())
    val uiState: StateFlow<PermissionDisabledUiState> = _uiState.asStateFlow()

    init {
        refreshPermissionState()
    }

    /**
     * Checks actual OS permission states and updates UI immediately.
     */
    fun refreshPermissionState() {
        val hasAccessibility = accessibilityPermissionChecker.isAccessibilityPermissionGranted()
        val hasUsage = usageStatsDataSource.hasUsageStatsPermission()

        _uiState.update {
            it.copy(
                isAccessibilityGranted = hasAccessibility,
                isUsageAccessGranted = hasUsage,
                isProtectionPaused = !hasAccessibility || !hasUsage
            )
        }
    }

    companion object {
        fun provideFactory(
            accessibilityPermissionChecker: AccessibilityPermissionChecker,
            usageStatsDataSource: UsageStatsDataSource
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PermissionDisabledViewModel(
                    accessibilityPermissionChecker = accessibilityPermissionChecker,
                    usageStatsDataSource = usageStatsDataSource
                ) as T
            }
        }
    }
}

/**
 * ViewModel for Screen 17 — Offline State (Figma Node 70:241).
 */
class OfflineViewModel(
    private val networkConnectivityObserver: NetworkConnectivityObserver,
    private val accessibilityPermissionChecker: AccessibilityPermissionChecker,
    private val usageStatsDataSource: UsageStatsDataSource,
    private val usageAggregator: UsageAggregator? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineUiState())
    val uiState: StateFlow<OfflineUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
        checkLocalProtection()
        loadUsage()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkConnectivityObserver.observeIsOnline().collect { isOnline ->
                _uiState.update { it.copy(isOnline = isOnline) }
            }
        }
    }

    fun checkLocalProtection() {
        val hasAccessibility = accessibilityPermissionChecker.isAccessibilityPermissionGranted()
        val hasUsage = usageStatsDataSource.hasUsageStatsPermission()
        // Protection is active on-device only if local permissions are intact
        val isLocallyProtected = hasAccessibility && hasUsage

        _uiState.update { it.copy(isLocallyProtected = isLocallyProtected) }
    }

    fun loadUsage(nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val todayMins = usageAggregator?.let {
                val summary = it.getTodayUsage(nowMillis)
                (summary.totalMonitoredMillis / 60000L).toInt()
            } ?: 0

            _uiState.update { current ->
                current.copy(
                    todayMonitoredMinutes = todayMins,
                    isReelsCountAvailable = false,
                    todayReelsWatched = null
                )
            }
        }
    }

    /**
     * Executes verifiable connectivity check upon user retry action.
     */
    fun retryConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRetrying = true, feedbackMessage = null) }
            val onlineNow = networkConnectivityObserver.isOnline()
            checkLocalProtection()

            _uiState.update {
                it.copy(
                    isOnline = onlineNow,
                    isRetrying = false,
                    feedbackMessage = if (onlineNow) {
                        "Connected to network"
                    } else {
                        "Still offline · Protection continues on-device"
                    }
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            networkConnectivityObserver: NetworkConnectivityObserver,
            accessibilityPermissionChecker: AccessibilityPermissionChecker,
            usageStatsDataSource: UsageStatsDataSource,
            usageAggregator: UsageAggregator? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OfflineViewModel(
                    networkConnectivityObserver = networkConnectivityObserver,
                    accessibilityPermissionChecker = accessibilityPermissionChecker,
                    usageStatsDataSource = usageStatsDataSource,
                    usageAggregator = usageAggregator
                ) as T
            }
        }
    }
}

/**
 * ViewModel for Screen 18 — First Day / Empty State (Figma Node 70:261).
 */
class FirstDayViewModel(
    private val usageAggregator: UsageAggregator? = null,
    private val usageStatsDataSource: UsageStatsDataSource? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirstDayUiState())
    val uiState: StateFlow<FirstDayUiState> = _uiState.asStateFlow()

    init {
        loadFirstDayState()
    }

    fun loadFirstDayState(nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val hasPermission = usageStatsDataSource?.hasUsageStatsPermission() ?: true
            val todaySummary = usageAggregator?.getTodayUsage(nowMillis)
            val todayMins = ((todaySummary?.totalMonitoredMillis ?: 0L) / 60000L).toInt()

            // If history is completely empty or 0 minutes monitored
            val isFirstDay = !hasPermission || todayMins == 0

            _uiState.update {
                it.copy(
                    isFirstDay = isFirstDay,
                    todayMonitoredMinutes = todayMins,
                    isReelsCountAvailable = false,
                    todayReelsWatched = if (todayMins == 0) 0 else null
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            usageAggregator: UsageAggregator? = null,
            usageStatsDataSource: UsageStatsDataSource? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FirstDayViewModel(
                    usageAggregator = usageAggregator,
                    usageStatsDataSource = usageStatsDataSource
                ) as T
            }
        }
    }
}
