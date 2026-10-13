package com.stillfeed.app.feature.edgestates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.feature.edgestates.connectivity.NetworkConnectivityObserver
import com.stillfeed.app.feature.edgestates.model.BatchFinishedUiState
import com.stillfeed.app.feature.edgestates.model.FirstDayUiState
import com.stillfeed.app.feature.edgestates.model.OfflineUiState
import com.stillfeed.app.feature.edgestates.model.PermissionDisabledUiState
import com.stillfeed.app.feature.edgestates.model.ProtectionRuntimeStatus
import com.stillfeed.app.protection.engine.ProtectionEngine
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
 * Requirements:
 * - Default [isBatchExpired] to false.
 * - Requires a verified batch-expiration event from Step 19.
 * - Does not independently expire or grant unlock sessions.
 * - Preserves existing Step 19 integration contract.
 */
class BatchFinishedViewModel(
    private val usageAggregator: UsageAggregator? = null,
    private val initialDailyAllowanceReels: Int = 30,
    initialBatchReels: Int? = null,
    initialBatchDurationMinutes: Int? = null,
    initialIsExpired: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BatchFinishedUiState(
            isBatchExpired = initialIsExpired,
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
     * Step 19 integration hook: records verified batch expiration event.
     */
    fun onBatchExpired(unlockedReels: Int?, durationMinutes: Int?) {
        _uiState.update { current ->
            current.copy(
                isBatchExpired = true,
                batchUnlockedReelsCount = unlockedReels,
                batchDurationMinutes = durationMinutes
            )
        }
    }

    /**
     * Preserves existing contract: updates expired batch and marks expired.
     */
    fun updateExpiredBatch(unlockedReels: Int?, durationMinutes: Int?) {
        onBatchExpired(unlockedReels, durationMinutes)
    }

    companion object {
        fun provideFactory(
            usageAggregator: UsageAggregator? = null,
            initialDailyAllowanceReels: Int = 30,
            initialBatchReels: Int? = null,
            initialBatchDurationMinutes: Int? = null,
            initialIsExpired: Boolean = false
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BatchFinishedViewModel(
                    usageAggregator = usageAggregator,
                    initialDailyAllowanceReels = initialDailyAllowanceReels,
                    initialBatchReels = initialBatchReels,
                    initialBatchDurationMinutes = initialBatchDurationMinutes,
                    initialIsExpired = initialIsExpired
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
 *
 * Requirements:
 * - Does not infer complete protection from permissions alone.
 * - Verifies available service and protection-enabled state using existing project APIs.
 * - Never claims "STILL PROTECTED" when service is stopped, protection is disabled, or state is unknown.
 */
class OfflineViewModel(
    private val networkConnectivityObserver: NetworkConnectivityObserver,
    private val accessibilityPermissionChecker: AccessibilityPermissionChecker,
    private val usageStatsDataSource: UsageStatsDataSource,
    private val protectionStateRepository: ProtectionStateRepository? = null,
    private val protectionEngine: ProtectionEngine? = null,
    private val isServiceRunningProvider: (() -> Boolean)? = null,
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
        viewModelScope.launch {
            val hasAccessibility = accessibilityPermissionChecker.isAccessibilityPermissionGranted()
            val hasUsage = usageStatsDataSource.hasUsageStatsPermission()

            if (!hasAccessibility || !hasUsage) {
                _uiState.update {
                    it.copy(
                        protectionStatus = ProtectionRuntimeStatus.PERMISSIONS_MISSING,
                        isLocallyProtected = false,
                        statusLabel = "PROTECTION PAUSED · Required permissions missing"
                    )
                }
                return@launch
            }

            // Check if protection is enabled in repository or engine
            val isProtectionEnabled = when {
                protectionEngine != null -> protectionEngine.isProtectionEnabled.value
                protectionStateRepository != null -> {
                    try {
                        protectionStateRepository.getProtectionState().isEnabled
                    } catch (_: Exception) {
                        false
                    }
                }
                else -> null // Protection enable state unknown
            }

            if (isProtectionEnabled == false) {
                _uiState.update {
                    it.copy(
                        protectionStatus = ProtectionRuntimeStatus.PROTECTION_DISABLED,
                        isLocallyProtected = false,
                        statusLabel = "PROTECTION PAUSED · Protection is turned off"
                    )
                }
                return@launch
            }

            // Check if the runtime service is actively running
            val isServiceActive = isServiceRunningProvider?.invoke()

            val (status, isProtected, label) = when {
                isProtectionEnabled == true && isServiceActive == true -> {
                    Triple(
                        ProtectionRuntimeStatus.PROTECTED,
                        true,
                        "STILL PROTECTED · On-device engine active"
                    )
                }
                isProtectionEnabled == true && isServiceActive == false -> {
                    Triple(
                        ProtectionRuntimeStatus.PROTECTION_DISABLED,
                        false,
                        "PROTECTION PAUSED · Accessibility service is stopped"
                    )
                }
                else -> {
                    // Runtime state cannot be fully verified
                    Triple(
                        ProtectionRuntimeStatus.PERMISSIONS_ENABLED_STATUS_UNVERIFIED,
                        false,
                        "Required permissions enabled; protection status unverified."
                    )
                }
            }

            _uiState.update {
                it.copy(
                    protectionStatus = status,
                    isLocallyProtected = isProtected,
                    statusLabel = label
                )
            }
        }
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
            protectionStateRepository: ProtectionStateRepository? = null,
            protectionEngine: ProtectionEngine? = null,
            isServiceRunningProvider: (() -> Boolean)? = null,
            usageAggregator: UsageAggregator? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OfflineViewModel(
                    networkConnectivityObserver = networkConnectivityObserver,
                    accessibilityPermissionChecker = accessibilityPermissionChecker,
                    usageStatsDataSource = usageStatsDataSource,
                    protectionStateRepository = protectionStateRepository,
                    protectionEngine = protectionEngine,
                    isServiceRunningProvider = isServiceRunningProvider,
                    usageAggregator = usageAggregator
                ) as T
            }
        }
    }
}

/**
 * ViewModel for Screen 18 — First Day / Empty State (Figma Node 70:261).
 *
 * Requirements:
 * - When Reel-count tracking is unavailable, keep todayReelsWatched = null.
 * - Do not convert zero monitored minutes into zero Reels watched.
 * - Distinguish unavailable Usage Access data from verified zero monitored time.
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
            val hasPermission = usageStatsDataSource?.hasUsageStatsPermission() ?: false

            if (!hasPermission) {
                _uiState.update {
                    it.copy(
                        isFirstDay = true,
                        isUsageAccessGranted = false,
                        isUsageDataAvailable = false,
                        todayMonitoredMinutes = null,
                        isReelsCountAvailable = false,
                        todayReelsWatched = null
                    )
                }
                return@launch
            }

            if (usageAggregator == null) {
                _uiState.update {
                    it.copy(
                        isFirstDay = true,
                        isUsageAccessGranted = true,
                        isUsageDataAvailable = false,
                        todayMonitoredMinutes = null,
                        isReelsCountAvailable = false,
                        todayReelsWatched = null
                    )
                }
                return@launch
            }

            try {
                val todaySummary = usageAggregator.getTodayUsage(nowMillis)
                val todayMins = (todaySummary.totalMonitoredMillis / 60000L).toInt()
                val isFirstDay = todayMins == 0

                _uiState.update {
                    it.copy(
                        isFirstDay = isFirstDay,
                        isUsageAccessGranted = true,
                        isUsageDataAvailable = true,
                        todayMonitoredMinutes = todayMins,
                        isReelsCountAvailable = false,
                        todayReelsWatched = null // Kept strictly null when single-reel tracking is unintercepted
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFirstDay = true,
                        isUsageAccessGranted = true,
                        isUsageDataAvailable = false,
                        todayMonitoredMinutes = null,
                        isReelsCountAvailable = false,
                        todayReelsWatched = null
                    )
                }
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
