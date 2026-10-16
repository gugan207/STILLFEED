package com.stillfeed.app.protection.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.repository.DefaultUnlockSessionRepository
import com.stillfeed.app.data.repository.LocalProtectionStateRepository
import com.stillfeed.app.data.repository.LocalUsageRepository
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.protection.detection.CompositeSurfaceClassifier
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import com.stillfeed.app.protection.model.ProtectionDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProtectionEngineUnlockIntegrationTest {

    private lateinit var database: StillfeedDatabase
    private lateinit var protectionRepo: LocalProtectionStateRepository
    private lateinit var usageRepo: LocalUsageRepository
    private lateinit var unlockRepo: DefaultUnlockSessionRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val createdEngines = mutableListOf<DefaultProtectionEngine>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        protectionRepo = LocalProtectionStateRepository(database.protectionStateDao(), testDispatcher)
        usageRepo = LocalUsageRepository(database.usageEventDao(), testDispatcher)
        unlockRepo = DefaultUnlockSessionRepository(
            activeUnlockSessionDao = database.activeUnlockSessionDao(),
            ioDispatcher = testDispatcher
        )
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
            unlockSessionRepository = unlockRepo,
            ioDispatcher = testDispatcher,
            coroutineScope = scope
        )
        createdEngines.add(engine)
        return engine
    }

    @Test
    fun protectionEnabled_withoutUnlock_blocksReel() = testScope.runTest {
        protectionRepo.setProtectionEnabled(true)
        val engine = createEngine(this)
        engine.isProtectionEnabled.first { it }

        val context = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/clips_viewer_view_pager")
        )

        val decision = engine.evaluateContext(context)
        assertTrue(decision is ProtectionDecision.Block)
        val block = decision as ProtectionDecision.Block
        assertEquals(SurfaceCategory.REELS, block.surfaceCategory)
        assertFalse("Unprompted block must not be flagged as batch expired", block.isBatchExpired)

        engine.close()
    }

    @Test
    fun protectionEnabled_withValidUnlock_allowsReelAndConsumesAllowance() = testScope.runTest {
        protectionRepo.setProtectionEnabled(true)
        val engine = createEngine(this)
        engine.isProtectionEnabled.first { it }

        // Create unlock batch of 5 reels
        unlockRepo.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 5,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        )

        val context = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/clips_viewer_view_pager")
        )

        // Evaluate: should ALLOW because of active unlock
        val decision = engine.evaluateContext(context)
        assertTrue(decision is ProtectionDecision.Allow)

        // Allowance should have been decremented from 5 to 4
        val active = unlockRepo.getActiveUnlock("com.instagram.android")
        assertEquals(4, active?.remainingBatchAllowance)

        engine.close()
    }

    @Test
    fun protectionEnabled_repeatedContentChanges_doNotDecrementAllowance() = testScope.runTest {
        protectionRepo.setProtectionEnabled(true)
        val engine = createEngine(this)
        engine.isProtectionEnabled.first { it }

        // Create unlock batch of 5 reels
        unlockRepo.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 5,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        )

        val context = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/clips_viewer_view_pager"),
            eventType = android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        )

        // First evaluation: entry into viewer -> 4 remaining
        val d1 = engine.evaluateContext(context)
        assertTrue(d1 is ProtectionDecision.Allow)
        assertEquals(4, unlockRepo.getActiveUnlock("com.instagram.android")?.remainingBatchAllowance)

        // Second evaluation: repeated content change on same Reel -> should NOT decrement
        val d2 = engine.evaluateContext(context)
        assertTrue(d2 is ProtectionDecision.Allow)
        assertEquals(4, unlockRepo.getActiveUnlock("com.instagram.android")?.remainingBatchAllowance)

        // Third evaluation: another content change on same Reel -> should NOT decrement
        val d3 = engine.evaluateContext(context)
        assertTrue(d3 is ProtectionDecision.Allow)
        assertEquals(4, unlockRepo.getActiveUnlock("com.instagram.android")?.remainingBatchAllowance)

        engine.close()
    }

    @Test
    fun protectionEnabled_whenUnlockExhausted_blocksReelAgain_andSetsBatchExpired() = testScope.runTest {
        protectionRepo.setProtectionEnabled(true)
        val engine = createEngine(this)
        engine.isProtectionEnabled.first { it }

        // Create unlock batch of 5 reels, then expire it
        unlockRepo.createUnlockSession(
            packageName = "com.google.android.youtube",
            batchAllowance = 5,
            activityType = "BREATHING",
            verifiedActivityCompletion = true
        )

        // Expire the unlock
        unlockRepo.expireUnlock("com.google.android.youtube")

        val ytContext = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = setOf("com.google.android.youtube:id/reel_recycler")
        )

        // Attempt should now BLOCK because session is expired/exhausted
        val decision = engine.evaluateContext(ytContext)
        assertTrue(decision is ProtectionDecision.Block)
        val block = decision as ProtectionDecision.Block
        assertTrue("Block after expired session must have isBatchExpired = true", block.isBatchExpired)
        assertEquals(5, block.batchAllowance)

        // Subsequent evaluation for unrelated package should NOT be marked expired
        val igContext = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/clips_viewer_view_pager")
        )
        val igDecision = engine.evaluateContext(igContext)
        assertTrue(igDecision is ProtectionDecision.Block)
        assertFalse("Unrelated package without expired session must have isBatchExpired = false", (igDecision as ProtectionDecision.Block).isBatchExpired)

        engine.close()
    }
}

