package com.stillfeed.app.usage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.repository.LocalUsageRepository
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import com.stillfeed.app.usage.analysis.ForegroundSession
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import com.stillfeed.app.usage.reconciliation.UsageReconciler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsageReconcilerTest {

    private lateinit var database: StillfeedDatabase
    private lateinit var usageRepository: LocalUsageRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var reconciler: UsageReconciler

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        usageRepository = LocalUsageRepository(database.usageEventDao(), testDispatcher)
        reconciler = UsageReconciler(usageRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun standardSession_isPersistedCorrectly() = testScope.runTest {
        val session = ForegroundSession(
            packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L
        )

        val persisted = reconciler.reconcile(listOf(session))

        assertEquals(1, persisted.size)
        val event = persisted[0]
        assertEquals(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, event.packageName)
        assertEquals(UsageEventType.SESSION_END, event.eventType)
        assertEquals(4000L, event.durationMillis)
        assertFalse(event.isBlocked)
        assertEquals(SurfaceCategory.FEED, event.surfaceCategory)

        // Verify in Room
        val count = usageRepository.getEventCount()
        assertEquals(1, count)
    }

    @Test
    fun duplicateSession_isPreventedFromDuplicateInsertion() = testScope.runTest {
        val session = ForegroundSession(
            packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
            startTimeMillis = 1000L,
            endTimeMillis = 5000L,
            durationMillis = 4000L
        )

        // First run inserts
        val firstRun = reconciler.reconcile(listOf(session))
        assertEquals(1, firstRun.size)

        // Subsequent run with same/overlapping session must NOT insert duplicate
        val secondRun = reconciler.reconcile(listOf(session))
        assertTrue("Duplicate session must not be inserted again", secondRun.isEmpty())

        val totalInRoom = usageRepository.getEventCount()
        assertEquals("Total Room events must remain 1", 1, totalInRoom)
    }

    @Test
    fun blockedIntervention_isReconciledWithoutDoubleCountingOrDurationInflation() = testScope.runTest {
        // Simulate an Accessibility Service BLOCKED event recorded at t=2500ms
        val blockedEvent = UsageEvent(
            id = UUID.randomUUID().toString(),
            timestampMillis = 2500L,
            packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
            surfaceCategory = SurfaceCategory.REELS,
            eventType = UsageEventType.BLOCKED,
            durationMillis = null,
            isBlocked = true,
            metadata = "Reels detected"
        )
        usageRepository.recordEvent(blockedEvent)

        // UsageStats reports a 2000ms session from 1000ms to 3000ms (during which intervention happened)
        val session = ForegroundSession(
            packageName = UsageSessionAnalyzer.PACKAGE_INSTAGRAM,
            startTimeMillis = 1000L,
            endTimeMillis = 3000L,
            durationMillis = 2000L
        )

        val persisted = reconciler.reconcile(listOf(session))

        assertEquals(1, persisted.size)
        val sessionEvent = persisted[0]
        assertTrue("Session overlapping intervention must be marked as blocked", sessionEvent.isBlocked)
        assertEquals(SurfaceCategory.REELS, sessionEvent.surfaceCategory)
        assertEquals("Duration must reflect exact UsageStats duration without inflation", 2000L, sessionEvent.durationMillis)
        assertEquals("Intervened by STILLFEED", sessionEvent.metadata)
    }

    @Test
    fun multipleRapidAccessibilityEvents_doNotCreateDuplicateSessions() = testScope.runTest {
        // Multiple rapid blocked events logged by Accessibility Service
        val block1 = UsageEvent(
            id = UUID.randomUUID().toString(),
            timestampMillis = 2000L,
            packageName = UsageSessionAnalyzer.PACKAGE_YOUTUBE,
            surfaceCategory = SurfaceCategory.SHORTS,
            eventType = UsageEventType.BLOCKED,
            isBlocked = true
        )
        val block2 = UsageEvent(
            id = UUID.randomUUID().toString(),
            timestampMillis = 2100L,
            packageName = UsageSessionAnalyzer.PACKAGE_YOUTUBE,
            surfaceCategory = SurfaceCategory.SHORTS,
            eventType = UsageEventType.BLOCKED,
            isBlocked = true
        )
        usageRepository.recordEvents(listOf(block1, block2))

        val session = ForegroundSession(
            packageName = UsageSessionAnalyzer.PACKAGE_YOUTUBE,
            startTimeMillis = 1500L,
            endTimeMillis = 2500L,
            durationMillis = 1000L
        )

        val persisted = reconciler.reconcile(listOf(session))

        assertEquals(1, persisted.size)
        assertTrue(persisted[0].isBlocked)
        assertEquals(SurfaceCategory.SHORTS, persisted[0].surfaceCategory)
    }
}
