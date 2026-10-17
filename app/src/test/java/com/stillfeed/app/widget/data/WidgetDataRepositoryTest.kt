package com.stillfeed.app.widget.data

import com.stillfeed.app.domain.model.AppPreferences
import com.stillfeed.app.domain.model.ProtectionState
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.domain.repository.AppPreferencesRepository
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.domain.repository.UsageRepository
import com.stillfeed.app.protection.permission.AccessibilityPermissionChecker
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import com.stillfeed.app.widget.model.WidgetTimeTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class WidgetDataRepositoryTest {

    private class FakeProtectionStateRepository(
        var state: ProtectionState = ProtectionState(isEnabled = true)
    ) : ProtectionStateRepository {
        override fun observeProtectionState(): Flow<ProtectionState> = flowOf(state)
        override suspend fun getProtectionState(): ProtectionState = state
        override suspend fun setProtectionEnabled(enabled: Boolean) {
            state = state.copy(isEnabled = enabled)
        }
        override suspend fun clearProtectionState() {
            state = ProtectionState(isEnabled = false)
        }
    }

    private class FakeAppPreferencesRepository(
        var preferences: AppPreferences = AppPreferences(dailyReelsAllowance = 45)
    ) : AppPreferencesRepository {
        override fun observePreferences(): Flow<AppPreferences> = flowOf(preferences)
        override suspend fun getPreferences(): AppPreferences = preferences
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setFirstLaunch(isFirstLaunch: Boolean) {}
        override suspend fun setQuickProtectionToggle(enabled: Boolean) {}
        override suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
            preferences = transform(preferences)
        }
        override suspend fun clearPreferences() {}
    }

    private class FakeAccessibilityChecker(
        var isGranted: Boolean = true
    ) : AccessibilityPermissionChecker {
        override fun isAccessibilityPermissionGranted(): Boolean = isGranted
    }

    private class FakeUsageRepository(
        private val events: List<UsageEvent> = emptyList()
    ) : UsageRepository {
        override suspend fun recordEvent(event: UsageEvent) {}
        override suspend fun recordEvents(events: List<UsageEvent>) {}
        override suspend fun getEventById(id: String): UsageEvent? = events.find { it.id == id }
        override fun observeEventsInRange(startMillis: Long, endMillis: Long): Flow<List<UsageEvent>> =
            flowOf(events.filter { it.timestampMillis in startMillis..endMillis })
        override suspend fun getEventsInRange(startMillis: Long, endMillis: Long): List<UsageEvent> =
            events.filter { it.timestampMillis in startMillis..endMillis }
        override suspend fun getEventCount(): Int = events.size
        override suspend fun deleteEventsOlderThan(cutoffMillis: Long): Int = 0
        override suspend fun clearUsageHistory() {}
    }

    @Test
    fun testDiurnalThemeResolutionAcrossDayPhases() {
        // Morning: 06:00 - 11:59
        assertEquals(WidgetTimeTheme.MORNING, WidgetDataRepository.resolveTheme(LocalTime.of(6, 0)))
        assertEquals(WidgetTimeTheme.MORNING, WidgetDataRepository.resolveTheme(LocalTime.of(9, 30)))
        assertEquals(WidgetTimeTheme.MORNING, WidgetDataRepository.resolveTheme(LocalTime.of(11, 59)))

        // Day: 12:00 - 15:59
        assertEquals(WidgetTimeTheme.DAY, WidgetDataRepository.resolveTheme(LocalTime.of(12, 0)))
        assertEquals(WidgetTimeTheme.DAY, WidgetDataRepository.resolveTheme(LocalTime.of(14, 15)))
        assertEquals(WidgetTimeTheme.DAY, WidgetDataRepository.resolveTheme(LocalTime.of(15, 59)))

        // Afternoon: 16:00 - 18:59
        assertEquals(WidgetTimeTheme.AFTERNOON, WidgetDataRepository.resolveTheme(LocalTime.of(16, 0)))
        assertEquals(WidgetTimeTheme.AFTERNOON, WidgetDataRepository.resolveTheme(LocalTime.of(17, 30)))
        assertEquals(WidgetTimeTheme.AFTERNOON, WidgetDataRepository.resolveTheme(LocalTime.of(18, 59)))

        // Evening: 19:00 - 21:59
        assertEquals(WidgetTimeTheme.EVENING, WidgetDataRepository.resolveTheme(LocalTime.of(19, 0)))
        assertEquals(WidgetTimeTheme.EVENING, WidgetDataRepository.resolveTheme(LocalTime.of(20, 45)))
        assertEquals(WidgetTimeTheme.EVENING, WidgetDataRepository.resolveTheme(LocalTime.of(21, 59)))

        // Night: 22:00 - 05:59
        assertEquals(WidgetTimeTheme.NIGHT, WidgetDataRepository.resolveTheme(LocalTime.of(22, 0)))
        assertEquals(WidgetTimeTheme.NIGHT, WidgetDataRepository.resolveTheme(LocalTime.of(23, 59)))
        assertEquals(WidgetTimeTheme.NIGHT, WidgetDataRepository.resolveTheme(LocalTime.of(0, 0)))
        assertEquals(WidgetTimeTheme.NIGHT, WidgetDataRepository.resolveTheme(LocalTime.of(3, 30)))
        assertEquals(WidgetTimeTheme.NIGHT, WidgetDataRepository.resolveTheme(LocalTime.of(5, 59)))
    }

    @Test
    fun testHonestUsageAggregation_neverFabricatesReelCount() = runTest {
        val now = System.currentTimeMillis()
        val events = listOf(
            UsageEvent(
                id = "evt-1",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = now - 60_000,
                durationMillis = 15 * 60_000L // 15 minutes
            ),
            UsageEvent(
                id = "evt-2",
                packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
                surfaceCategory = com.stillfeed.app.domain.model.SurfaceCategory.REELS,
                eventType = UsageEventType.BLOCKED,
                timestampMillis = now - 30_000,
                isBlocked = true
            )
        )

        val usageRepo = FakeUsageRepository(events)
        val usageAggregator = UsageAggregator(usageRepo)
        val protectionRepo = FakeProtectionStateRepository(ProtectionState(isEnabled = true))
        val prefsRepo = FakeAppPreferencesRepository(AppPreferences(dailyReelsAllowance = 30))
        val accessibilityChecker = FakeAccessibilityChecker(isGranted = true)

        val repository = WidgetDataRepository(
            usageAggregator = usageAggregator,
            protectionStateRepository = protectionRepo,
            appPreferencesRepository = prefsRepo,
            accessibilityPermissionChecker = accessibilityChecker
        )

        val state = repository.getWidgetUiState(currentTime = LocalTime.of(8, 0), nowMillis = now)

        assertEquals(WidgetTimeTheme.MORNING, state.theme)
        assertEquals(15L, state.monitoredMinutes)
        assertEquals(1, state.blockedInterventions)
        assertEquals(30, state.dailyAllowanceMinutes)
        // Data honesty: reel count is unmeasured / null, resulting in em-dash "—"
        assertNull(state.reelCount)
        assertEquals("—", state.displayCount)
        assertTrue(state.isFullyProtected)
        assertEquals("Your mind looks fresh.", state.editorialMessage)
    }

    @Test
    fun testProtectionPausedMessageWhenAccessibilityMissing() = runTest {
        val usageRepo = FakeUsageRepository()
        val usageAggregator = UsageAggregator(usageRepo)
        val protectionRepo = FakeProtectionStateRepository(ProtectionState(isEnabled = true))
        val prefsRepo = FakeAppPreferencesRepository()
        val accessibilityChecker = FakeAccessibilityChecker(isGranted = false) // Permission disabled

        val repository = WidgetDataRepository(
            usageAggregator = usageAggregator,
            protectionStateRepository = protectionRepo,
            appPreferencesRepository = prefsRepo,
            accessibilityPermissionChecker = accessibilityChecker
        )

        val state = repository.getWidgetUiState(currentTime = LocalTime.of(14, 0))

        assertFalse(state.isFullyProtected)
        assertEquals("Protection paused · Tap to resume", state.editorialMessage)
    }

    @Test
    fun testProtectionPausedMessageWhenProtectionDisabled() = runTest {
        val usageRepo = FakeUsageRepository()
        val usageAggregator = UsageAggregator(usageRepo)
        val protectionRepo = FakeProtectionStateRepository(ProtectionState(isEnabled = false)) // Protection paused
        val prefsRepo = FakeAppPreferencesRepository()
        val accessibilityChecker = FakeAccessibilityChecker(isGranted = true)

        val repository = WidgetDataRepository(
            usageAggregator = usageAggregator,
            protectionStateRepository = protectionRepo,
            appPreferencesRepository = prefsRepo,
            accessibilityPermissionChecker = accessibilityChecker
        )

        val state = repository.getWidgetUiState(currentTime = LocalTime.of(17, 0))

        assertFalse(state.isFullyProtected)
        assertEquals("Protection paused · Tap to resume", state.editorialMessage)
    }

    @Test
    fun testEditorialMessagesByDiurnalPhase() = runTest {
        val usageRepo = FakeUsageRepository()
        val usageAggregator = UsageAggregator(usageRepo)
        val protectionRepo = FakeProtectionStateRepository(ProtectionState(isEnabled = true))
        val prefsRepo = FakeAppPreferencesRepository()
        val accessibilityChecker = FakeAccessibilityChecker(isGranted = true)

        val repository = WidgetDataRepository(
            usageAggregator = usageAggregator,
            protectionStateRepository = protectionRepo,
            appPreferencesRepository = prefsRepo,
            accessibilityPermissionChecker = accessibilityChecker
        )

        assertEquals("Your mind looks fresh.", repository.getWidgetUiState(currentTime = LocalTime.of(7, 0)).editorialMessage)
        assertEquals("Nice and slow. You’re doing well.", repository.getWidgetUiState(currentTime = LocalTime.of(13, 0)).editorialMessage)
        assertEquals("You’ve been here a while.", repository.getWidgetUiState(currentTime = LocalTime.of(17, 0)).editorialMessage)
        assertEquals("Take a breath. You can still reset.", repository.getWidgetUiState(currentTime = LocalTime.of(20, 0)).editorialMessage)
        assertEquals("Rest well. You’ve done enough.", repository.getWidgetUiState(currentTime = LocalTime.of(23, 0)).editorialMessage)
    }
}
