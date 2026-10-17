package com.stillfeed.app.protection.intervention.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stillfeed.app.domain.model.BatchUnlockSession
import com.stillfeed.app.domain.repository.UnlockSessionRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.protection.intervention.model.AwarenessCard
import com.stillfeed.app.protection.intervention.model.AwarenessCardCatalog
import com.stillfeed.app.protection.intervention.model.ResetActivityType
import com.stillfeed.app.protection.sensor.PhoneOrientationState
import com.stillfeed.app.protection.sensor.ResetSensorManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InterventionStep {
    BLOCKED_REEL,              // Screen 08 — Blocked Reel (39:162)
    DM_REEL_BLOCKED,           // Screen 14 — Reel from DM Blocked (70:184)
    RESET_SELECTION,           // Screen 09 — Reset Activity list (64:7)
    RESET_IN_PROGRESS,         // Screen 09 — Interactive countdown execution
    AWARENESS_CARDS,           // Screen 10 — Awareness Cards reflection (64:46)
    UNLOCK_SELECTION,          // Screen 11 — Unlock batch choices (64:65)
    BATCH_FINISHED             // Screen 15 — Daily Limit / Daily Finished (70:198)
}

data class InterventionUiState(
    val currentStep: InterventionStep = InterventionStep.BLOCKED_REEL,
    val blockedPackage: String = "",
    val surfaceCategory: String = "Reel",
    val todayReelsCount: Int? = null,
    val batchUnlockedReelsCount: Int? = null,
    val batchDurationMinutes: Int? = null,
    // Step 09 Reset Activity State
    val selectedActivity: ResetActivityType = ResetActivityType.FLIP_PHONE,
    val isActivityTimerRunning: Boolean = false,
    val activityRemainingSeconds: Int = 30,
    val isActivityCompleted: Boolean = false,
    // Sensor Flip Phone State
    val isPhoneFaceDown: Boolean = false,
    val phoneRemovedNotice: String? = null,
    // Step 10 Awareness Cards State
    val currentCardIndex: Int = 0,
    val cards: List<AwarenessCard> = AwarenessCardCatalog.cards,
    val isCardReflectionComplete: Boolean = false,
    val cardHoldRemainingSeconds: Int = 2,
    val isCardHoldActive: Boolean = true,
    // Step 11 Unlock Selection State
    val selectedBatchAllowance: Int = 20,
    val isSubmittingUnlock: Boolean = false,
    val isUnlockSuccessful: Boolean = false,
    val grantedSession: BatchUnlockSession? = null,
    val errorMessage: String? = null
) {
    val currentCard: AwarenessCard
        get() = cards.getOrElse(currentCardIndex) { cards.first() }

    val isLastCard: Boolean
        get() = currentCardIndex >= cards.size - 1
}

/**
 * ViewModel managing the full Step 19 intervention-to-unlock journey.
 */
class InterventionViewModel(
    private val unlockSessionRepository: UnlockSessionRepository,
    private val usageRepository: UsageRepository? = null,
    initialPackage: String = "",
    initialSurface: String = "Reel",
    private val initialStep: InterventionStep = InterventionStep.BLOCKED_REEL,
    initialBatchAllowance: Int? = null,
    initialBatchDurationMinutes: Int? = null,
    private val resetSensorManager: ResetSensorManager? = null,
    private val onResetReelCount: (() -> Unit)? = null,
    initialReelCount: Int? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InterventionUiState(
            currentStep = initialStep,
            blockedPackage = initialPackage,
            surfaceCategory = initialSurface,
            batchUnlockedReelsCount = initialBatchAllowance,
            batchDurationMinutes = initialBatchDurationMinutes
        )
    )
    val uiState: StateFlow<InterventionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sensorJob: Job? = null
    private var cardHoldJob: Job? = null

    init {
        if (initialReelCount != null && initialReelCount > 0) {
            _uiState.update { it.copy(todayReelsCount = initialReelCount) }
        } else {
            loadTodayReelCount()
        }
    }

    private fun loadTodayReelCount() {
        viewModelScope.launch {
            try {
                if (usageRepository == null) {
                    _uiState.update { it.copy(todayReelsCount = null) }
                    return@launch
                }
                // Use user's local calendar day rather than UTC midnight
                val localCalendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val startOfLocalDay = localCalendar.timeInMillis
                val now = System.currentTimeMillis()

                val events = usageRepository.getEventsInRange(startOfLocalDay, now)

                // Audit: STEP 19 Counting Contract
                // 1. Blocked attempts (isBlocked == true, BLOCKED) measure interventions, NOT watched reels.
                // 2. Monitored app sessions (SESSION_END) measure foreground minutes, NOT individual reels.
                // 3. Batch grants or unlock states (UNLOCKED, isUnlocked) measure permissions, NOT watched reels.
                // 4. An event can ONLY be counted if its model and producer explicitly establish verified individual Reel consumption.
                val verifiedIndividualReelEvents = events
                    .filter { isVerifiedIndividualReelConsumption(it) }
                    .distinctBy { it.id }

                if (verifiedIndividualReelEvents.isNotEmpty()) {
                    _uiState.update { it.copy(todayReelsCount = verifiedIndividualReelEvents.size) }
                } else {
                    // When reliable individual Reel-consumption tracking is unavailable (Step 19),
                    // keep count null and hide numeric badge / show designed clean reset state.
                    _uiState.update { it.copy(todayReelsCount = null) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(todayReelsCount = null) }
            }
        }
    }

    /**
     * Determines whether a [UsageEvent] explicitly represents an individual watched Reel.
     *
     * Audit Guarantees:
     * - Returns false for blocked attempts (isBlocked == true, BLOCKED).
     * - Returns false for session duration / app usage minutes (SESSION_END, SESSION_START).
     * - Returns false for unlock grants or permission changes (UNLOCKED, isUnlocked).
     * - Only returns true if an established producer explicitly marks the event as an individual Reel consumption.
     */
    private fun isVerifiedIndividualReelConsumption(event: com.stillfeed.app.domain.model.UsageEvent): Boolean {
        if (event.isBlocked || event.eventType == com.stillfeed.app.domain.model.UsageEventType.BLOCKED) return false
        if (event.eventType == com.stillfeed.app.domain.model.UsageEventType.SESSION_START ||
            event.eventType == com.stillfeed.app.domain.model.UsageEventType.SESSION_END) return false
        if (event.isUnlocked || event.eventType == com.stillfeed.app.domain.model.UsageEventType.UNLOCKED) return false

        return event.metadata == VERIFIED_REEL_CONSUMPTION_METADATA
    }


    // ==================== SCREEN 08: BLOCKED REEL ACTIONS ====================

    /**
     * User chooses "Choose a reset" (WATCH REELS) to proceed to Screen 09.
     */
    fun onChooseReset() {
        _uiState.update { it.copy(currentStep = InterventionStep.RESET_SELECTION) }
    }

    // ==================== SCREEN 09: RESET ACTIVITY ACTIONS ====================

    fun onSelectActivity(activity: ResetActivityType) {
        _uiState.update {
            it.copy(
                selectedActivity = activity,
                activityRemainingSeconds = activity.durationSeconds
            )
        }
    }

    fun onStartSelectedActivity() {
        val activity = _uiState.value.selectedActivity
        if (activity == ResetActivityType.AWARENESS_CARDS) {
            // Jump directly to Awareness Cards if selected as reset
            _uiState.update {
                it.copy(
                    isActivityCompleted = true,
                    currentStep = InterventionStep.AWARENESS_CARDS
                )
            }
            startCardHoldTimer()
            return
        }

        val duration = activity.durationSeconds
        _uiState.update {
            it.copy(
                currentStep = InterventionStep.RESET_IN_PROGRESS,
                isActivityTimerRunning = true,
                activityRemainingSeconds = duration,
                isActivityCompleted = false,
                isPhoneFaceDown = false,
                phoneRemovedNotice = null
            )
        }

        timerJob?.cancel()
        sensorJob?.cancel()

        if (activity != ResetActivityType.AWARENESS_CARDS && resetSensorManager != null) {
            resetSensorManager.startListening()
            var currentRemaining = duration

            sensorJob = viewModelScope.launch {
                resetSensorManager.orientationState.collect { orientation ->
                    when (orientation) {
                        PhoneOrientationState.FACE_DOWN -> {
                            _uiState.update {
                                it.copy(
                                    isPhoneFaceDown = true,
                                    phoneRemovedNotice = null
                                )
                            }
                            if (timerJob?.isActive != true) {
                                timerJob = viewModelScope.launch {
                                    while (currentRemaining > 0) {
                                        delay(1000L)
                                        currentRemaining--
                                        _uiState.update { it.copy(activityRemainingSeconds = currentRemaining) }
                                    }
                                    // Completed while face down!
                                    resetSensorManager.stopListening()
                                    _uiState.update {
                                        it.copy(
                                            isActivityTimerRunning = false,
                                            isActivityCompleted = true
                                        )
                                    }
                                    delay(600L)
                                    _uiState.update { it.copy(currentStep = InterventionStep.AWARENESS_CARDS) }
                                    startCardHoldTimer()
                                }
                            }
                        }
                        PhoneOrientationState.REMOVED_FROM_SURFACE -> {
                            // Rule: "if we remove the phone we need to start from first and as same as all activitys"
                            timerJob?.cancel()
                            timerJob = null
                            currentRemaining = duration
                            _uiState.update {
                                it.copy(
                                    isPhoneFaceDown = false,
                                    activityRemainingSeconds = duration,
                                    phoneRemovedNotice = "Phone lifted! Place phone face down to start over from ${duration}s."
                                )
                            }
                        }
                        else -> {
                            if (_uiState.value.isPhoneFaceDown) {
                                timerJob?.cancel()
                                timerJob = null
                                currentRemaining = duration
                                _uiState.update {
                                    it.copy(
                                        isPhoneFaceDown = false,
                                        activityRemainingSeconds = duration,
                                        phoneRemovedNotice = "Phone moved! Place phone face down to start over from ${duration}s."
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // General timer for other activities; if interrupted, restarts from first
            var remaining = duration
            timerJob = viewModelScope.launch {
                while (remaining > 0) {
                    delay(1000L)
                    remaining--
                    _uiState.update { it.copy(activityRemainingSeconds = remaining) }
                }
                _uiState.update {
                    it.copy(
                        isActivityTimerRunning = false,
                        isActivityCompleted = true
                    )
                }
                delay(600L)
                _uiState.update { it.copy(currentStep = InterventionStep.AWARENESS_CARDS) }
                startCardHoldTimer()
            }
        }
    }

    fun onCancelActivityTimer() {
        timerJob?.cancel()
        timerJob = null
        sensorJob?.cancel()
        sensorJob = null
        resetSensorManager?.stopListening()
        val defaultDuration = _uiState.value.selectedActivity.durationSeconds
        _uiState.update {
            it.copy(
                isActivityTimerRunning = false,
                isActivityCompleted = false,
                isPhoneFaceDown = false,
                phoneRemovedNotice = null,
                activityRemainingSeconds = defaultDuration,
                currentStep = InterventionStep.RESET_SELECTION
            )
        }
    }

    // ==================== SCREEN 10: AWARENESS CARDS ACTIONS ====================

    fun startCardHoldTimer() {
        cardHoldJob?.cancel()
        _uiState.update {
            it.copy(
                cardHoldRemainingSeconds = 2,
                isCardHoldActive = true
            )
        }
        cardHoldJob = viewModelScope.launch {
            delay(1000L)
            _uiState.update { it.copy(cardHoldRemainingSeconds = 1) }
            delay(1000L)
            _uiState.update { it.copy(cardHoldRemainingSeconds = 0, isCardHoldActive = false) }
        }
    }

    fun onNextCard() {
        val currentState = _uiState.value
        if (currentState.isCardHoldActive) return // Enforce 2 seconds pause before next card/scroll!

        if (currentState.isLastCard) {
            onFinishAwarenessCards()
        } else {
            _uiState.update { it.copy(currentCardIndex = it.currentCardIndex + 1) }
            startCardHoldTimer()
        }
    }

    fun onFinishAwarenessCards() {
        _uiState.update {
            it.copy(
                isCardReflectionComplete = true,
                currentStep = InterventionStep.UNLOCK_SELECTION
            )
        }
    }

    // ==================== SCREEN 11: UNLOCK SELECTION ACTIONS ====================

    fun onSelectBatchAllowance(allowance: Int) {
        if (BatchUnlockSession.SUPPORTED_BATCH_OPTIONS.contains(allowance)) {
            _uiState.update { it.copy(selectedBatchAllowance = allowance) }
        }
    }

    fun onConfirmUnlock() {
        val state = _uiState.value
        if (state.isSubmittingUnlock || state.isUnlockSuccessful) return

        if (!state.isActivityCompleted && !state.isCardReflectionComplete) {
            _uiState.update { it.copy(errorMessage = "Must complete reset activity before unlocking.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingUnlock = true, errorMessage = null) }
            val result = unlockSessionRepository.createUnlockSession(
                packageName = state.blockedPackage,
                batchAllowance = state.selectedBatchAllowance,
                activityType = state.selectedActivity.name,
                verifiedActivityCompletion = true
            )

            result.onSuccess { session ->
                onResetReelCount?.invoke()
                _uiState.update {
                    it.copy(
                        isSubmittingUnlock = false,
                        isUnlockSuccessful = true,
                        grantedSession = session
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmittingUnlock = false,
                        errorMessage = error.message ?: "Failed to grant unlock session"
                    )
                }
            }
        }
    }

    // ==================== BACK NAVIGATION NAVIGATION ====================

    /**
     * Handles system or in-app back navigation across the 4-step sequence.
     * Returns true if handled internally, or false if the Activity should finish.
     */
    fun handleBackNavigation(): Boolean {
        when (_uiState.value.currentStep) {
            InterventionStep.BLOCKED_REEL -> {
                // Exit intervention without unlocking
                return false
            }
            InterventionStep.DM_REEL_BLOCKED -> {
                // Exit intervention without unlocking, back to messages
                return false
            }
            InterventionStep.BATCH_FINISHED -> {
                // Exit intervention without unlocking
                return false
            }
            InterventionStep.RESET_SELECTION -> {
                if (initialStep == InterventionStep.RESET_SELECTION) {
                    return false
                }
                if (initialStep == InterventionStep.BATCH_FINISHED) {
                    _uiState.update { it.copy(currentStep = InterventionStep.BATCH_FINISHED) }
                    return true
                }
                _uiState.update { it.copy(currentStep = InterventionStep.BLOCKED_REEL) }
                return true
            }
            InterventionStep.RESET_IN_PROGRESS -> {
                onCancelActivityTimer()
                return true
            }
            InterventionStep.AWARENESS_CARDS -> {
                if (_uiState.value.currentCardIndex > 0) {
                    _uiState.update { it.copy(currentCardIndex = it.currentCardIndex - 1) }
                } else {
                    _uiState.update { it.copy(currentStep = InterventionStep.RESET_SELECTION) }
                }
                return true
            }
            InterventionStep.UNLOCK_SELECTION -> {
                _uiState.update { it.copy(currentStep = InterventionStep.AWARENESS_CARDS) }
                return true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        sensorJob?.cancel()
        cardHoldJob?.cancel()
        resetSensorManager?.stopListening()
    }

    companion object {
        const val VERIFIED_REEL_CONSUMPTION_METADATA = "INDIVIDUAL_REEL_CONSUMED"

        fun provideFactory(
            unlockSessionRepository: UnlockSessionRepository,
            usageRepository: UsageRepository?,
            blockedPackage: String,
            surfaceCategory: String,
            initialStep: InterventionStep = InterventionStep.BLOCKED_REEL,
            initialBatchAllowance: Int? = null,
            initialBatchDurationMinutes: Int? = null,
            resetSensorManager: ResetSensorManager? = null,
            onResetReelCount: (() -> Unit)? = null,
            initialReelCount: Int? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InterventionViewModel(
                    unlockSessionRepository = unlockSessionRepository,
                    usageRepository = usageRepository,
                    initialPackage = blockedPackage,
                    initialSurface = surfaceCategory,
                    initialStep = initialStep,
                    initialBatchAllowance = initialBatchAllowance,
                    initialBatchDurationMinutes = initialBatchDurationMinutes,
                    resetSensorManager = resetSensorManager,
                    onResetReelCount = onResetReelCount,
                    initialReelCount = initialReelCount
                ) as T
            }
        }
    }
}
