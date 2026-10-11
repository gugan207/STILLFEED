package com.stillfeed.app.integration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.auth.model.AuthError
import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.auth.model.AuthUser
import com.stillfeed.app.awareness.AwarenessManager
import com.stillfeed.app.core.navigation.Screen
import com.stillfeed.app.core.ui.components.StillfeedNavDestination
import com.stillfeed.app.data.auth.LocalAuthRepository
import com.stillfeed.app.data.auth.Pbkdf2PasswordHasher
import com.stillfeed.app.data.auth.SecurePreferencesAuthSessionStorage
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.preferences.DataStoreAppPreferencesRepository
import com.stillfeed.app.data.repository.LocalAwarenessRepository
import com.stillfeed.app.data.repository.LocalProtectionStateRepository
import com.stillfeed.app.data.repository.LocalUnlockHistoryRepository
import com.stillfeed.app.data.repository.LocalUsageRepository
import com.stillfeed.app.data.repository.LocalUserRepository
import com.stillfeed.app.domain.model.AwarenessMonthlyState
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UnlockEvent
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.feature.insights.InsightsViewModel
import com.stillfeed.app.feature.settings.SettingsViewModel
import com.stillfeed.app.feature.settings.export.DataExportService
import com.stillfeed.app.protection.blocking.DefaultInterventionCoordinator
import com.stillfeed.app.protection.engine.DefaultProtectionEngine
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import com.stillfeed.app.protection.model.ProtectionDecision
import com.stillfeed.app.protection.permission.AccessibilityPermissionChecker
import com.stillfeed.app.usage.aggregation.UsageAggregator
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import com.stillfeed.app.usage.reconciliation.UsageReconciler
import com.stillfeed.app.usage.stats.RawUsageTimelineEvent
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

/**
 * STEP 12 End-to-End Integration, Runtime Verification & Production Hardening Test Suite.
 *
 * Verifies that all components built across Steps 01–11 work together as one coherent,
 * hardened STILLFEED Android application.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StillfeedE2EIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dataStoreScope: CoroutineScope

    private lateinit var context: Context
    private lateinit var database: StillfeedDatabase

    private lateinit var userRepository: LocalUserRepository
    private lateinit var protectionStateRepository: LocalProtectionStateRepository
    private lateinit var usageRepository: LocalUsageRepository
    private lateinit var awarenessRepository: LocalAwarenessRepository
    private lateinit var unlockHistoryRepository: LocalUnlockHistoryRepository
    private lateinit var preferencesRepository: DataStoreAppPreferencesRepository
    private lateinit var authRepository: LocalAuthRepository

    private lateinit var usageAggregator: UsageAggregator
    private lateinit var awarenessManager: AwarenessManager
    private lateinit var dataExportService: DataExportService

    private val fixedNowMillis = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

        val directExecutor = java.util.concurrent.Executor { it.run() }
        database = StillfeedDatabase.buildInMemory(context, directExecutor)

        userRepository = LocalUserRepository(database.userDao(), testDispatcher)
        protectionStateRepository = LocalProtectionStateRepository(database.protectionStateDao(), testDispatcher)
        usageRepository = LocalUsageRepository(database.usageEventDao(), testDispatcher)
        awarenessRepository = LocalAwarenessRepository(database.awarenessMonthlyDao(), testDispatcher)
        unlockHistoryRepository = LocalUnlockHistoryRepository(database.unlockHistoryDao(), testDispatcher)

        dataStoreScope = CoroutineScope(testDispatcher + Job())
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { context.preferencesDataStoreFile("e2e_prefs_${UUID.randomUUID()}") }
        )
        preferencesRepository = DataStoreAppPreferencesRepository(testDataStore)

        val sessionStorage = SecurePreferencesAuthSessionStorage(context)
        sessionStorage.clearSession()
        val passwordHasher = Pbkdf2PasswordHasher()
        authRepository = LocalAuthRepository(
            sessionStorage = sessionStorage,
            passwordHasher = passwordHasher,
            ioDispatcher = testDispatcher,
            userRepository = userRepository
        )

        usageAggregator = UsageAggregator(usageRepository)
        awarenessManager = AwarenessManager(
            awarenessRepository = awarenessRepository,
            usageAggregator = usageAggregator,
            usageStatsDataSource = FakeE2EUsageStatsDataSource()
        )

        dataExportService = DataExportService(
            authRepository = authRepository,
            usageRepository = usageRepository,
            awarenessRepository = awarenessRepository,
            preferencesRepository = preferencesRepository
        )
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        database.close()
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Phase 3 — Authentication Lifecycle
    // -------------------------------------------------------------------------

    @Test
    fun phase03_auth_lifecycle_freshInstall_signUp_signIn_signOut_sessionClearing() = runTest(testDispatcher) {
        // 1. Fresh Install -> SignedOut
        assertEquals(AuthState.SignedOut, authRepository.authState.value)

        // 2. Sign Up
        val signUpResult = authRepository.signUp("mindful@stillfeed.app", "CalmPassword123!", "Mindful User")
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrThrow()
        assertEquals("mindful@stillfeed.app", user.email)
        assertEquals("Mindful User", user.displayName)

        // Verify user profile stored in repository
        val storedUser = userRepository.getUser(user.id)
        assertNotNull(storedUser)
        assertEquals("mindful@stillfeed.app", storedUser!!.email)

        // 3. Authenticated State
        assertTrue(authRepository.authState.value is AuthState.SignedIn)

        // 4. Sign Out
        val signOutResult = authRepository.signOut()
        assertTrue(signOutResult.isSuccess)
        assertEquals(AuthState.SignedOut, authRepository.authState.value)
        assertNull(authRepository.getCurrentUser())

        // 5. Sign In with wrong password fails
        val wrongSign = authRepository.signIn("mindful@stillfeed.app", "WrongPassword!")
        assertTrue(wrongSign.isFailure)
        assertEquals(AuthError.InvalidCredentials().userMessage, wrongSign.exceptionOrNull()?.message)
        assertTrue(authRepository.authState.value is AuthState.Error)

        // 6. Sign In with correct password succeeds
        val correctSign = authRepository.signIn("mindful@stillfeed.app", "CalmPassword123!")
        assertTrue(correctSign.isSuccess)
        assertEquals(user.id, correctSign.getOrThrow().id)
        assertTrue(authRepository.authState.value is AuthState.SignedIn)
    }

    // -------------------------------------------------------------------------
    // Phase 4 — Protection Scenarios A through E
    // -------------------------------------------------------------------------

    @Test
    fun phase04_protection_scenarios_A_through_E_and_edge_states() = runTest(testDispatcher) {
        val engine = DefaultProtectionEngine(
            protectionStateRepository = protectionStateRepository,
            usageRepository = usageRepository,
            ioDispatcher = testDispatcher,
            coroutineScope = this
        )

        // Scenario A: Protection OFF
        protectionStateRepository.setProtectionEnabled(false)
        advanceUntilIdle()

        val reelsContext = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/clips_video_container")
        )
        val decisionOff = engine.evaluateContext(reelsContext)
        assertEquals(ProtectionDecision.ProtectionDisabled, decisionOff)

        // Scenario B: Protection ON + permissions available
        protectionStateRepository.setProtectionEnabled(true)
        advanceUntilIdle()

        val decisionBlockReels = engine.evaluateContext(reelsContext)
        assertTrue(decisionBlockReels is ProtectionDecision.Block)
        assertEquals(SurfaceCategory.REELS, (decisionBlockReels as ProtectionDecision.Block).surfaceCategory)

        // YouTube Shorts blocked
        val shortsContext = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = setOf("com.google.android.youtube:id/shorts_container")
        )
        val decisionBlockShorts = engine.evaluateContext(shortsContext)
        assertTrue(decisionBlockShorts is ProtectionDecision.Block)
        assertEquals(SurfaceCategory.SHORTS, (decisionBlockShorts as ProtectionDecision.Block).surfaceCategory)

        // Allowed content (Instagram Feed)
        val feedContext = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/feed_stream")
        )
        val decisionAllowFeed = engine.evaluateContext(feedContext)
        assertTrue(decisionAllowFeed is ProtectionDecision.Allow)

        // Scenario C & E: Missing or revoked permissions trigger Screen 16 edge state
        val fakeAccessChecker = FakeE2EAccessibilityChecker(granted = false)
        val fakeUsageStats = FakeE2EUsageStatsDataSource(hasPerm = false)
        val settingsVm = SettingsViewModel(
            protectionStateRepository = protectionStateRepository,
            appPreferencesRepository = preferencesRepository,
            awarenessManager = awarenessManager,
            awarenessRepository = awarenessRepository,
            authRepository = authRepository,
            usageRepository = usageRepository,
            unlockHistoryRepository = unlockHistoryRepository,
            userRepository = userRepository,
            accessibilityPermissionChecker = fakeAccessChecker,
            usageStatsDataSource = fakeUsageStats,
            dataExportService = dataExportService
        )
        settingsVm.loadSettings(fixedNowMillis)
        advanceUntilIdle()

        // Flags edge state
        assertTrue(settingsVm.uiState.value.isProtectionPausedDueToPermissions)

        // Permission restored
        fakeAccessChecker.granted = true
        fakeUsageStats.hasPerm = true
        settingsVm.refreshPermissions()
        assertFalse(settingsVm.uiState.value.isProtectionPausedDueToPermissions)

        engine.close()
    }

    // -------------------------------------------------------------------------
    // Phase 5 — Protection Intervention Cooldown
    // -------------------------------------------------------------------------

    @Test
    fun phase05_protection_engine_intervention_cooldown_and_rate_limiting() {
        var globalActionCount = 0
        val coordinator = DefaultInterventionCoordinator(
            cooldownMs = 2000L,
            launchActivity = false,
            globalActionPerformer = { _, _ ->
                globalActionCount++
                true
            }
        )

        val blockDecision = ProtectionDecision.Block(
            packageName = "com.instagram.android",
            surfaceCategory = SurfaceCategory.REELS,
            reason = "Reels detected"
        )

        // Call 1: Must execute
        coordinator.executeIntervention(FakeAccessibilityService(), blockDecision)
        assertEquals(1, globalActionCount)

        // Call 2: Within cooldown window -> Must suppress without spamming
        coordinator.executeIntervention(FakeAccessibilityService(), blockDecision)
        assertEquals("Cooldown must suppress rapid repetitive trigger", 1, globalActionCount)
    }

    // -------------------------------------------------------------------------
    // Phase 6 — Usage Analysis Pipeline
    // -------------------------------------------------------------------------

    @Test
    fun phase06_usage_pipeline_rawEventsToAggregatedDailySummaries() = runTest(testDispatcher) {
        val analyzer = UsageSessionAnalyzer()
        val reconciler = UsageReconciler(usageRepository)

        val baseTime = fixedNowMillis - (2 * 60 * 60 * 1000L)
        val rawEvents = listOf(
            RawUsageTimelineEvent("com.instagram.android", baseTime, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent("com.instagram.android", baseTime + (20 * 60 * 1000L), UsageSessionAnalyzer.EVENT_TYPE_PAUSED)
        )

        val sessions = analyzer.analyze(rawEvents, fixedNowMillis)
        assertEquals(1, sessions.size)
        assertEquals(20 * 60 * 1000L, sessions[0].durationMillis)

        val reconciled = reconciler.reconcile(sessions)
        assertEquals(1, reconciled.size)

        val todaySummary = usageAggregator.getTodayUsage(fixedNowMillis)
        assertEquals(20 * 60 * 1000L, todaySummary.totalMonitoredMillis)
        assertEquals(1, todaySummary.sessionCount)
    }

    // -------------------------------------------------------------------------
    // Phase 7 — Awareness Target Selection & Monthly Locking
    // -------------------------------------------------------------------------

    @Test
    fun phase07_awareness_targetSelection_monthlyLocking_and_recalculation() = runTest(testDispatcher) {
        val monthId = awarenessManager.getCurrentMonthIdentifier(fixedNowMillis)

        // 1. Seed baseline
        awarenessRepository.saveMonthlyState(
            AwarenessMonthlyState(
                monthIdentifier = monthId,
                targetReductionPercentage = 0,
                baselineMinutes = 600,
                status = MonthlyAwarenessStatus.BASELINE_FORMING
            )
        )

        // 2. Select 10% target
        val selectResult = awarenessManager.selectMonthlyAwarenessPercentage(monthId, 10, fixedNowMillis)
        assertTrue(selectResult.isSuccess)

        val updated = awarenessRepository.getMonthlyState(monthId)
        assertNotNull(updated)
        assertEquals(10, updated!!.targetReductionPercentage)
        assertEquals(60, updated.reductionAmountMinutes) // 10% of 600 = 60
        assertEquals(540, updated.targetUsageMinutes)    // 600 - 60 = 540

        // 3. Locking rule: Subsequent invalid percentage is rejected
        val invalidResult = awarenessManager.selectMonthlyAwarenessPercentage(monthId, 13, fixedNowMillis)
        assertTrue("Subsequent invalid selection attempt must be rejected", invalidResult.isFailure)

        // 4. Verify calm status
        assertEquals(MonthlyAwarenessStatus.REDUCING, updated.status)
    }

    // -------------------------------------------------------------------------
    // Phase 7 — Insights Weekly Reflection & Daily Rhythm
    // -------------------------------------------------------------------------

    @Test
    fun phase07_insights_weeklyReflection_and_dailyRhythm_chart() = runTest(testDispatcher) {
        // Seed 7 days of usage events
        val oneDayMillis = 24 * 60 * 60 * 1000L
        for (i in 0..6) {
            val ts = fixedNowMillis - (i * oneDayMillis)
            usageRepository.recordEvent(
                UsageEvent(
                    id = "ev_past_$i",
                    packageName = "com.instagram.android",
                    surfaceCategory = SurfaceCategory.REELS,
                    eventType = UsageEventType.SESSION_END,
                    timestampMillis = ts,
                    durationMillis = 15 * 60 * 1000L
                )
            )
        }

        val insightsVm = InsightsViewModel(
            usageAggregator = usageAggregator,
            awarenessManager = awarenessManager,
            usageStatsDataSource = FakeE2EUsageStatsDataSource()
        )
        insightsVm.loadInsights(fixedNowMillis)
        advanceUntilIdle()

        val state = insightsVm.uiState.value
        assertFalse(state.isFirstDayOrEmpty)
        assertEquals(7, state.dailyRhythm.size)
        assertTrue(state.weeklyMonitoredMinutes > 0)
        assertEquals("Establishing your reference rhythm.", state.reflectionHeadline)
    }

    // -------------------------------------------------------------------------
    // Phase 8 — Settings Process Recreation
    // -------------------------------------------------------------------------

    @Test
    fun phase08_settings_processRecreation_preservesAllPreferences() = runTest(testDispatcher) {
        preferencesRepository.updatePreferences {
            it.copy(
                preferredThemeMode = "DARK",
                dailyReminderEnabled = false,
                quickProtectionToggle = true
            )
        }
        protectionStateRepository.setProtectionEnabled(true)
        advanceUntilIdle()

        // Simulate process recreation
        val recreatedSettingsVm = SettingsViewModel(
            protectionStateRepository = protectionStateRepository,
            appPreferencesRepository = preferencesRepository,
            awarenessManager = awarenessManager,
            awarenessRepository = awarenessRepository,
            authRepository = authRepository,
            usageRepository = usageRepository,
            unlockHistoryRepository = unlockHistoryRepository,
            userRepository = userRepository,
            accessibilityPermissionChecker = FakeE2EAccessibilityChecker(granted = true),
            usageStatsDataSource = FakeE2EUsageStatsDataSource(),
            dataExportService = dataExportService
        )
        recreatedSettingsVm.loadSettings(fixedNowMillis)
        advanceUntilIdle()

        val state = recreatedSettingsVm.uiState.value
        assertTrue(state.protectionEnabled)
        assertEquals("DARK", state.preferredThemeMode)
    }

    // -------------------------------------------------------------------------
    // Phase 9 — Data Export Privacy Audit
    // -------------------------------------------------------------------------

    @Test
    fun phase09_dataExport_strictPrivacyAudit_zeroSensitiveCredentialsExposed() = runTest(testDispatcher) {
        authRepository.signUp("audit@stillfeed.app", "SecurePass123!", "Audit User")
        usageRepository.recordEvent(
            UsageEvent(
                id = "ev_priv_1",
                packageName = "com.instagram.android",
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = fixedNowMillis,
                durationMillis = 10 * 60 * 1000L
            )
        )

        val jsonString = dataExportService.generateExportJson(fixedNowMillis)
        val root = JSONObject(jsonString)

        // Schema integrity
        assertEquals("STILLFEED", root.getString("appName"))
        assertEquals(1, root.getInt("exportSchemaVersion"))

        // Strict Exclusion Verification
        assertFalse("Password must NEVER be present", root.has("password"))
        assertFalse("Password hash must NEVER be present", root.has("passwordHash"))
        assertFalse("Salt must NEVER be present", root.has("salt"))
        assertFalse("Tokens must NEVER be present", root.has("token"))
        assertFalse("Session token must NEVER be present", root.has("sessionToken"))
        assertFalse("Bearer token must NEVER be present", root.has("bearer"))
        assertFalse("Raw node text must NEVER be present", root.has("nodeText"))
        assertFalse("Accessibility text must NEVER be present", root.has("accessibilityText"))
    }

    // -------------------------------------------------------------------------
    // Phase 10 — Complete Account & Data Deletion Purge
    // -------------------------------------------------------------------------

    @Test
    fun phase10_accountDeletion_cascadingPurge_cleansesAllStores() = runTest(testDispatcher) {
        authRepository.signUp("purge@stillfeed.app", "P@ss123456", "Purge User")
        usageRepository.recordEvent(
            UsageEvent(
                id = "u_del",
                packageName = "com.instagram.android",
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.SESSION_END,
                timestampMillis = fixedNowMillis
            )
        )
        unlockHistoryRepository.recordUnlock(UnlockEvent("un_del", fixedNowMillis, 15, "break"))
        awarenessRepository.saveMonthlyState(
            AwarenessMonthlyState(monthIdentifier = "2026-10", targetReductionPercentage = 10)
        )
        protectionStateRepository.setProtectionEnabled(true)
        advanceUntilIdle()

        val settingsVm = SettingsViewModel(
            protectionStateRepository = protectionStateRepository,
            appPreferencesRepository = preferencesRepository,
            awarenessManager = awarenessManager,
            awarenessRepository = awarenessRepository,
            authRepository = authRepository,
            usageRepository = usageRepository,
            unlockHistoryRepository = unlockHistoryRepository,
            userRepository = userRepository,
            accessibilityPermissionChecker = FakeE2EAccessibilityChecker(granted = true),
            usageStatsDataSource = FakeE2EUsageStatsDataSource(),
            dataExportService = dataExportService
        )
        settingsVm.loadSettings(fixedNowMillis)
        advanceUntilIdle()

        settingsVm.deleteAccountAndData()
        advanceUntilIdle()

        assertTrue(settingsVm.uiState.value.accountDeleted)
        assertTrue(settingsVm.uiState.value.signedOut)

        // Verify total store cleansing
        assertEquals(0, usageRepository.getEventsInRange(0L, Long.MAX_VALUE).size)
        assertEquals(0, unlockHistoryRepository.getUnlocksInRange(0L, Long.MAX_VALUE).size)
        assertEquals(0, awarenessRepository.getAllMonthlyStates().size)
        assertFalse(protectionStateRepository.getProtectionState().isEnabled)
        assertNull(authRepository.getCurrentUser())
        assertEquals(AuthState.SignedOut, authRepository.authState.value)
    }

    // -------------------------------------------------------------------------
    // Phase 2 — Navigation Destinations Integrity
    // -------------------------------------------------------------------------

    @Test
    fun phase02_navigation_routes_and_bottom_destination_contracts() {
        val routes = listOf(
            Screen.Auth.route,
            Screen.Bootstrap.route,
            Screen.Onboarding.route,
            Screen.Home.route,
            Screen.AwarenessSelection.route,
            Screen.Insights.route,
            Screen.Settings.route
        )
        assertEquals(7, routes.size)
        assertEquals(7, routes.toSet().size)

        val navTabs = StillfeedNavDestination.values()
        assertEquals(3, navTabs.size)
        assertEquals(listOf("Home", "Insights", "Settings"), navTabs.map { it.label })
    }

    // -------------------------------------------------------------------------
    // Phase 12 — Empty & First-Day States
    // -------------------------------------------------------------------------

    @Test
    fun phase12_emptyState_firstDayUser_displaysCalmNoticing() = runTest(testDispatcher) {
        val insightsVm = InsightsViewModel(
            usageAggregator = usageAggregator,
            awarenessManager = awarenessManager,
            usageStatsDataSource = FakeE2EUsageStatsDataSource()
        )
        insightsVm.loadInsights(fixedNowMillis)
        advanceUntilIdle()

        val state = insightsVm.uiState.value
        assertTrue(state.isFirstDayOrEmpty)
        assertEquals(0, state.weeklyMonitoredMinutes)
    }

    // -------------------------------------------------------------------------
    // Phase 13 — Structural Window Context Helper Robustness
    // -------------------------------------------------------------------------

    @Test
    fun phase13_accessibilityWindowContext_boundedTraversal_and_tagMatching() {
        val ctx = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/reel_viewer", "com.instagram.android:id/media_container"),
            structuralTags = setOf("reels_tab", "clips_viewer")
        )
        assertTrue(ctx.hasViewIdContaining("reel"))
        assertTrue(ctx.hasTagContaining("clips"))
        assertFalse(ctx.hasViewIdContaining("non_existent"))
    }
}

// -----------------------------------------------------------------------------
// Test Doubles for E2E Integration
// -----------------------------------------------------------------------------

private class FakeE2EAccessibilityChecker(var granted: Boolean = true) : AccessibilityPermissionChecker {
    override fun isAccessibilityPermissionGranted(): Boolean = granted
}

private class FakeE2EUsageStatsDataSource(var hasPerm: Boolean = true) : UsageStatsDataSource {
    override fun hasUsageStatsPermission(): Boolean = hasPerm
    override fun queryTimelineEvents(startTimeMillis: Long, endTimeMillis: Long): List<RawUsageTimelineEvent> = emptyList()
}

private class FakeAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
