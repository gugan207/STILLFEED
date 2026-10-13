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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InterventionStep {
    BLOCKED_REEL,              // Screen 08 — Blocked Reel (39:162)
    RESET_SELECTION,           // Screen 09 — Reset Activity list (64:7)
    RESET_IN_PROGRESS,         // Screen 09 — Interactive countdown execution
    AWARENESS_CARDS,           // Screen 10 — Awareness Cards reflection (64:46)
    UNLOCK_SELECTION           // Screen 11 — Unlock batch choices (64:65)
}

data class InterventionUiState(
    val currentStep: InterventionStep = InterventionStep.BLOCKED_REEL,
    val blockedPackage: String = "",
    val surfaceCategory: String = "Reel",
    val todayReelsCount: Int? = null,
    // Step 09 Reset Activity State
    val selectedActivity: ResetActivityType = ResetActivityType.FLIP_PHONE,
    val isActivityTimerRunning: Boolean = false,
    val activityRemainingSeconds: Int = 30,
    val isActivityCompleted: Boolean = false,
    // Step 10 Awareness Cards State
    val currentCardIndex: Int = 0,
    val cards: List<AwarenessCard> = AwarenessCardCatalog.cards,
    val isCardReflectionComplete: Boolean = false,
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
    initialSurface: String = "Reel"
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InterventionUiState(
            blockedPackage = initialPackage,
            surfaceCategory = initialSurface
        )
    )
    val uiState: StateFlow<InterventionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadTodayReelCount()
    }

    private fun loadTodayReelCount() {
        viewModelScope.launch {
            try {
                if (usageRepository == null) {
                    _uiState.update { it.copy(todayReelsCount = null) }
                    return@launch
                }
                // Requirement 1: Use user's local calendar day rather than UTC midnight
                val localCalendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val startOfLocalDay = localCalendar.timeInMillis
                val now = System.currentTimeMillis()

                val events = usageRepository.getEventsInRange(startOfLocalDay, now)
                // Requirement 1: Only count genuine watched-Reel events (e.g. UNLOCKED),
                // NEVER count isBlocked == true (which measures blocked attempts).
                val verifiedWatchedEvents = events.filter { it.isUnlocked || it.eventType == com.stillfeed.app.domain.model.UsageEventType.UNLOCKED }

                if (verifiedWatchedEvents.isNotEmpty()) {
                    _uiState.update { it.copy(todayReelsCount = verifiedWatchedEvents.size) }
                } else {
                    // When reliable watched-Reel tracking data is unavailable, keep null
                    _uiState.update { it.copy(todayReelsCount = null) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(todayReelsCount = null) }
            }
        }
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
            return
        }

        val duration = activity.durationSeconds
        _uiState.update {
            it.copy(
                currentStep = InterventionStep.RESET_IN_PROGRESS,
                isActivityTimerRunning = true,
                activityRemainingSeconds = duration,
                isActivityCompleted = false
            )
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = duration
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.update { it.copy(activityRemainingSeconds = remaining) }
            }
            // Verified completion!
            _uiState.update {
                it.copy(
                    isActivityTimerRunning = false,
                    isActivityCompleted = true
                )
            }
            // Small pause for completion feedback before moving to reflection
            delay(600L)
            _uiState.update { it.copy(currentStep = InterventionStep.AWARENESS_CARDS) }
        }
    }

    fun onCancelActivityTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update {
            it.copy(
                isActivityTimerRunning = false,
                isActivityCompleted = false,
                currentStep = InterventionStep.RESET_SELECTION
            )
        }
    }

    // ==================== SCREEN 10: AWARENESS CARDS ACTIONS ====================

    fun onNextCard() {
        val currentState = _uiState.value
        if (currentState.isLastCard) {
            onFinishAwarenessCards()
        } else {
            _uiState.update { it.copy(currentCardIndex = it.currentCardIndex + 1) }
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
            InterventionStep.RESET_SELECTION -> {
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
    }

    companion object {
        fun provideFactory(
            unlockSessionRepository: UnlockSessionRepository,
            usageRepository: UsageRepository?,
            blockedPackage: String,
            surfaceCategory: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InterventionViewModel(
                    unlockSessionRepository = unlockSessionRepository,
                    usageRepository = usageRepository,
                    initialPackage = blockedPackage,
                    initialSurface = surfaceCategory
                ) as T
            }
        }
    }
}
