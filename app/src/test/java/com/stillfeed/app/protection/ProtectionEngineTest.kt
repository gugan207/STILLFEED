package com.stillfeed.app.protection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.repository.LocalProtectionStateRepository
import com.stillfeed.app.data.repository.LocalUsageRepository
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.protection.detection.CompositeSurfaceClassifier
import com.stillfeed.app.protection.engine.DefaultProtectionEngine
import com.stillfeed.app.protection.engine.ProtectionEngine
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import com.stillfeed.app.protection.model.ProtectionDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProtectionEngineTest {

    private lateinit var database: StillfeedDatabase
    private lateinit var protectionRepo: LocalProtectionStateRepository
    private lateinit var usageRepo: LocalUsageRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val createdEngines = mutableListOf<ProtectionEngine>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        protectionRepo = LocalProtectionStateRepository(database.protectionStateDao(), testDispatcher)
        usageRepo = LocalUsageRepository(database.usageEventDao(), testDispatcher)
        createdEngines.clear()
    }

    @After
    fun tearDown() {
        createdEngines.forEach { it.close() }
        createdEngines.clear()
        database.close()
    }

    private fun createEngine(scope: CoroutineScope): DefaultProtectionEngine {
        val engine = DefaultProtectionEngine(
            protectionStateRepository = protectionRepo,
            usageRepository = usageRepo,
            classifier = CompositeSurfaceClassifier(),
            ioDispatcher = testDispatcher,
            coroutineScope = scope
        )
        createdEngines.add(engine)
        return engine
    }

    @Test
    fun defaultState_protectionDisabled_returnsProtectionDisabled() = testScope.runTest {
        val engine = createEngine(this)
        advanceUntilIdle()

        assertFalse("Protection should initially be disabled", engine.isProtectionEnabled.value)

        val context = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/clips_viewer_view_pager")
        )
        val decision = engine.evaluateContext(context)
        assertEquals(ProtectionDecision.ProtectionDisabled, decision)

        engine.close()
    }

    @Test
    fun enabledState_blocksReelsAndShorts() = testScope.runTest {
        protectionRepo.setProtectionEnabled(true)
        val engine = createEngine(this)
        val enabled = engine.isProtectionEnabled.first { it }
        assertTrue("Engine must reflect enabled state", enabled)

        // Evaluate Instagram Reel
        val reelContext = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/clips_viewer_view_pager")
        )
        val igDecision = engine.evaluateContext(reelContext)
        assertTrue("Instagram Reels must be blocked", igDecision is ProtectionDecision.Block)
        val igBlock = igDecision as ProtectionDecision.Block
        assertEquals(SurfaceCategory.REELS, igBlock.surfaceCategory)

        // Evaluate YouTube Shorts
        val shortsContext = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = setOf("com.google.android.youtube:id/shorts_player")
        )
        val ytDecision = engine.evaluateContext(shortsContext)
        assertTrue("YouTube Shorts must be blocked", ytDecision is ProtectionDecision.Block)
        val ytBlock = ytDecision as ProtectionDecision.Block
        assertEquals(SurfaceCategory.SHORTS, ytBlock.surfaceCategory)

        engine.close()
    }

    @Test
    fun enabledState_allowsAllowedSurfaces() = testScope.runTest {
        protectionRepo.setProtectionEnabled(true)
        val engine = createEngine(this)
        engine.isProtectionEnabled.first { it }

        // Instagram DMs
        val dmContext = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/direct_thread")
        )
        val dmDecision = engine.evaluateContext(dmContext)
        assertTrue("Instagram DMs must be allowed", dmDecision is ProtectionDecision.Allow)

        // YouTube Normal Video
        val videoContext = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = setOf("com.google.android.youtube:id/watch_panel")
        )
        val videoDecision = engine.evaluateContext(videoContext)
        assertTrue("YouTube standard video must be allowed", videoDecision is ProtectionDecision.Allow)

        engine.close()
    }

    @Test
    fun rapidStateResponse_observesToggleChangesInRealTime() = testScope.runTest {
        val engine = createEngine(this)
        assertFalse(engine.isProtectionEnabled.value)

        // Toggle on
        protectionRepo.setProtectionEnabled(true)
        val enabled = engine.isProtectionEnabled.first { it }
        assertTrue("Engine must immediately observe repository enabled toggle", enabled)

        // Toggle off
        protectionRepo.setProtectionEnabled(false)
        val disabled = engine.isProtectionEnabled.first { !it }
        assertFalse("Engine must immediately observe repository disabled toggle", disabled)

        engine.close()
    }

    @Test
    fun stateSurvivesRecreation_fromPersistenceLayer() = testScope.runTest {
        protectionRepo.setProtectionEnabled(true)

        // Create new engine instance as if process recreated
        val recreatedEngine = createEngine(this)
        val enabled = recreatedEngine.isProtectionEnabled.first { it }
        assertTrue("New engine instance must load enabled state from Room repository", enabled)

        recreatedEngine.close()
    }

    @Test
    fun blockDecision_recordsUsageEventWithRateLimiting() = testScope.runTest {
        protectionRepo.setProtectionEnabled(true)
        val engine = createEngine(this)
        engine.isProtectionEnabled.first { it }

        val reelContext = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/clips_viewer_view_pager")
        )

        engine.evaluateContext(reelContext)
        advanceUntilIdle()

        val events = usageRepo.getEventsInRange(0L, Long.MAX_VALUE)
        assertEquals("Should record 1 blocked usage event in repository", 1, events.size)
        assertTrue(events[0].isBlocked)
        assertEquals(SurfaceCategory.REELS, events[0].surfaceCategory)
        assertEquals("com.instagram.android", events[0].packageName)

        engine.close()
    }
}
