package com.stillfeed.app.protection.intervention.ui

import com.stillfeed.app.data.repository.DefaultUnlockSessionRepository
import com.stillfeed.app.protection.intervention.model.ResetActivityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InterventionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var unlockRepository: DefaultUnlockSessionRepository
    private lateinit var viewModel: InterventionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        unlockRepository = DefaultUnlockSessionRepository(ioDispatcher = testDispatcher)
        viewModel = InterventionViewModel(
            unlockSessionRepository = unlockRepository,
            usageRepository = null,
            initialPackage = "com.instagram.android",
            initialSurface = "Reel"
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isBlockedReel_withDefaults() {
        val state = viewModel.uiState.value
        assertEquals(InterventionStep.BLOCKED_REEL, state.currentStep)
        assertEquals("com.instagram.android", state.blockedPackage)
        assertEquals("Reel", state.surfaceCategory)
        assertEquals(ResetActivityType.FLIP_PHONE, state.selectedActivity)
        assertEquals(20, state.selectedBatchAllowance)
        assertFalse(state.isActivityCompleted)
        assertFalse(state.isCardReflectionComplete)
        assertFalse(state.isUnlockSuccessful)
        assertNull(state.todayReelsCount)
    }

    @Test
    fun onChooseReset_navigatesToResetSelection() {
        viewModel.onChooseReset()
        assertEquals(InterventionStep.RESET_SELECTION, viewModel.uiState.value.currentStep)
    }

    @Test
    fun onSelectActivity_updatesSelectedActivityAndDuration() {
        viewModel.onChooseReset()
        viewModel.onSelectActivity(ResetActivityType.MOVE_BODY)

        val state = viewModel.uiState.value
        assertEquals(ResetActivityType.MOVE_BODY, state.selectedActivity)
        assertEquals(ResetActivityType.MOVE_BODY.durationSeconds, state.activityRemainingSeconds)
    }

    @Test
    fun onStartSelectedActivity_timerFlow_completesAndAdvances() = runTest {
        viewModel.onChooseReset()
        viewModel.onSelectActivity(ResetActivityType.FLIP_PHONE)
        viewModel.onStartSelectedActivity()

        var state = viewModel.uiState.value
        assertEquals(InterventionStep.RESET_IN_PROGRESS, state.currentStep)
        assertTrue(state.isActivityTimerRunning)
        assertFalse(state.isActivityCompleted)

        // Advance timer completely (30 seconds + 600ms transition delay)
        advanceTimeBy(31_000L)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue("Activity must be marked completed after timer finishes", state.isActivityCompleted)
        assertEquals(InterventionStep.AWARENESS_CARDS, state.currentStep)
    }

    @Test
    fun onCancelActivityTimer_abortsAndDoesNotGrantCompletion() = runTest {
        viewModel.onChooseReset()
        viewModel.onSelectActivity(ResetActivityType.FLIP_PHONE)
        viewModel.onStartSelectedActivity()

        advanceTimeBy(10_000L) // 10 seconds in
        viewModel.onCancelActivityTimer()

        val state = viewModel.uiState.value
        assertEquals(InterventionStep.RESET_SELECTION, state.currentStep)
        assertFalse("Timer must not be running", state.isActivityTimerRunning)
        assertFalse("Activity must NOT be completed when cancelled", state.isActivityCompleted)
    }

    @Test
    fun startAwarenessCardsDirectly_completesActivityAndAdvances() {
        viewModel.onChooseReset()
        viewModel.onSelectActivity(ResetActivityType.AWARENESS_CARDS)
        viewModel.onStartSelectedActivity()

        val state = viewModel.uiState.value
        assertEquals(InterventionStep.AWARENESS_CARDS, state.currentStep)
        assertTrue(state.isActivityCompleted)
    }

    @Test
    fun awarenessCardProgression_completesAndNavigatesToUnlock() {
        viewModel.onChooseReset()
        viewModel.onSelectActivity(ResetActivityType.AWARENESS_CARDS)
        viewModel.onStartSelectedActivity()

        // Advance through cards
        val totalCards = viewModel.uiState.value.cards.size
        for (i in 0 until totalCards) {
            viewModel.onNextCard()
        }

        val state = viewModel.uiState.value
        assertTrue(state.isCardReflectionComplete)
        assertEquals(InterventionStep.UNLOCK_SELECTION, state.currentStep)
    }

    @Test
    fun onSelectBatchAllowance_validOptions_updatesAllowance() {
        viewModel.onSelectBatchAllowance(5)
        assertEquals(5, viewModel.uiState.value.selectedBatchAllowance)

        viewModel.onSelectBatchAllowance(10)
        assertEquals(10, viewModel.uiState.value.selectedBatchAllowance)

        viewModel.onSelectBatchAllowance(30)
        assertEquals(30, viewModel.uiState.value.selectedBatchAllowance)

        // Invalid allowance should be rejected
        viewModel.onSelectBatchAllowance(15)
        assertEquals(30, viewModel.uiState.value.selectedBatchAllowance)
    }

    @Test
    fun onConfirmUnlock_withoutCompletion_isRejected() = runTest {
        viewModel.onConfirmUnlock()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isUnlockSuccessful)
        assertNotNull(state.errorMessage)
        assertNull(state.grantedSession)
    }

    @Test
    fun onConfirmUnlock_withVerifiedCompletion_grantsBatchSession() = runTest {
        // Complete via Awareness cards
        viewModel.onChooseReset()
        viewModel.onSelectActivity(ResetActivityType.AWARENESS_CARDS)
        viewModel.onStartSelectedActivity()
        viewModel.onFinishAwarenessCards()

        viewModel.onSelectBatchAllowance(10)
        viewModel.onConfirmUnlock()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isUnlockSuccessful)
        assertNotNull(state.grantedSession)
        assertEquals("com.instagram.android", state.grantedSession?.packageName)
        assertEquals(10, state.grantedSession?.selectedBatchAllowance)
        assertEquals(10, state.grantedSession?.remainingBatchAllowance)
    }

    @Test
    fun handleBackNavigation_respectsStepHierarchy() = runTest {
        // 1. In BLOCKED_REEL -> handleBackNavigation returns false (finish activity)
        assertFalse(viewModel.handleBackNavigation())

        // 2. In RESET_SELECTION -> handleBackNavigation goes back to BLOCKED_REEL
        viewModel.onChooseReset()
        assertEquals(InterventionStep.RESET_SELECTION, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.handleBackNavigation())
        assertEquals(InterventionStep.BLOCKED_REEL, viewModel.uiState.value.currentStep)

        // 3. In RESET_IN_PROGRESS -> handleBackNavigation cancels timer and returns to RESET_SELECTION
        viewModel.onChooseReset()
        viewModel.onSelectActivity(ResetActivityType.FLIP_PHONE)
        viewModel.onStartSelectedActivity()
        assertEquals(InterventionStep.RESET_IN_PROGRESS, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.handleBackNavigation())
        assertEquals(InterventionStep.RESET_SELECTION, viewModel.uiState.value.currentStep)
        assertFalse(viewModel.uiState.value.isActivityCompleted)

        // 4. In AWARENESS_CARDS -> handleBackNavigation returns to RESET_SELECTION when at first card
        viewModel.onSelectActivity(ResetActivityType.AWARENESS_CARDS)
        viewModel.onStartSelectedActivity()
        assertEquals(InterventionStep.AWARENESS_CARDS, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.handleBackNavigation())
        assertEquals(InterventionStep.RESET_SELECTION, viewModel.uiState.value.currentStep)
    }

    @Test
    fun loadTodayReelCount_ignoresBlockedAttempts_andKeepsCountNull() = runTest {
        val fakeUsage = FakeUsageRepository()
        // Record 5 blocked attempt events (isBlocked = true)
        val now = System.currentTimeMillis()
        for (i in 1..5) {
            fakeUsage.recordEvent(
                com.stillfeed.app.domain.model.UsageEvent(
                    id = "blocked_$i",
                    timestampMillis = now,
                    packageName = "com.instagram.android",
                    surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.REELS,
                    eventType = com.stillfeed.app.domain.model.UsageEventType.BLOCKED,
                    isBlocked = true
                )
            )
        }

        val vm = InterventionViewModel(
            unlockSessionRepository = unlockRepository,
            usageRepository = fakeUsage,
            initialPackage = "com.instagram.android"
        )
        advanceUntilIdle()

        // Requirement 1: isBlocked == true measures blocked attempts rather than watched Reels.
        // It must NOT count blocked attempts as watched reels!
        assertNull("Blocked attempts must not masquerade as watched reels", vm.uiState.value.todayReelsCount)
    }

    @Test
    fun loadTodayReelCount_countsOnlyWatchedReels_withinLocalCalendarDay() = runTest {
        val fakeUsage = FakeUsageRepository()
        val now = System.currentTimeMillis()

        // 1. Two genuine watched reels today (eventType = UNLOCKED, isBlocked = false)
        fakeUsage.recordEvent(
            com.stillfeed.app.domain.model.UsageEvent(
                id = "watched_1",
                timestampMillis = now - 60_000L,
                packageName = "com.instagram.android",
                surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.REELS,
                eventType = com.stillfeed.app.domain.model.UsageEventType.UNLOCKED,
                isBlocked = false,
                isUnlocked = true
            )
        )
        fakeUsage.recordEvent(
            com.stillfeed.app.domain.model.UsageEvent(
                id = "watched_2",
                timestampMillis = now - 30_000L,
                packageName = "com.instagram.android",
                surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.REELS,
                eventType = com.stillfeed.app.domain.model.UsageEventType.UNLOCKED,
                isBlocked = false,
                isUnlocked = true
            )
        )

        // 2. One watched reel yesterday (prior to local midnight)
        val localMidnight = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        fakeUsage.recordEvent(
            com.stillfeed.app.domain.model.UsageEvent(
                id = "watched_yesterday",
                timestampMillis = localMidnight - 3600_000L,
                packageName = "com.instagram.android",
                surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.REELS,
                eventType = com.stillfeed.app.domain.model.UsageEventType.UNLOCKED,
                isBlocked = false,
                isUnlocked = true
            )
        )

        val vm = InterventionViewModel(
            unlockSessionRepository = unlockRepository,
            usageRepository = fakeUsage,
            initialPackage = "com.instagram.android"
        )
        advanceUntilIdle()

        // Only the 2 reels watched today in the local calendar day are counted
        assertEquals(2, vm.uiState.value.todayReelsCount)
    }

    private class FakeUsageRepository : com.stillfeed.app.domain.repository.UsageRepository {
        val events = mutableListOf<com.stillfeed.app.domain.model.UsageEvent>()
        override suspend fun recordEvent(event: com.stillfeed.app.domain.model.UsageEvent) { events.add(event) }
        override suspend fun recordEvents(events: List<com.stillfeed.app.domain.model.UsageEvent>) { this.events.addAll(events) }
        override suspend fun getEventById(id: String) = events.find { it.id == id }
        override fun observeEventsInRange(startMillis: Long, endMillis: Long) = kotlinx.coroutines.flow.flowOf(events.filter { it.timestampMillis in startMillis..endMillis })
        override suspend fun getEventsInRange(startMillis: Long, endMillis: Long) = events.filter { it.timestampMillis in startMillis..endMillis }
        override suspend fun getEventCount() = events.size
        override suspend fun deleteEventsOlderThan(cutoffMillis: Long) = 0
        override suspend fun clearUsageHistory() { events.clear() }
    }
}

