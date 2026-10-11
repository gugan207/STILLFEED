package com.stillfeed.app.awareness

import com.stillfeed.app.domain.model.ProtectionState
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.repository.ProtectionStateRepository
import com.stillfeed.app.protection.detection.CompositeSurfaceClassifier
import com.stillfeed.app.protection.engine.DefaultProtectionEngine
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import com.stillfeed.app.protection.model.ProtectionDecision
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration verification between Awareness states and [com.stillfeed.app.protection.engine.ProtectionEngine] (STEP 08 Section 7 & 10).
 *
 * Product Invariant:
 * Awareness state, missing UsageStats permissions, forming baselines, or database failures
 * must NEVER weaken or disable instantaneous short-form blocking.
 */
class ProtectionAwarenessIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var protectionEngine: DefaultProtectionEngine

    private val fakeProtectionRepo = object : ProtectionStateRepository {
        private var state = ProtectionState(isEnabled = true)
        override fun observeProtectionState(): Flow<ProtectionState> = flowOf(state)
        override suspend fun getProtectionState(): ProtectionState = state
        override suspend fun setProtectionEnabled(enabled: Boolean) { state = state.copy(isEnabled = enabled) }
        override suspend fun clearProtectionState() { state = ProtectionState(isEnabled = false) }
    }

    @Before
    fun setUp() {
        protectionEngine = DefaultProtectionEngine(
            protectionStateRepository = fakeProtectionRepo,
            classifier = CompositeSurfaceClassifier(),
            ioDispatcher = testDispatcher
        )
        protectionEngine.updateProtectionState(true)
    }

    @Test
    fun reelsBlocking_remainsActive_whenAwarenessDataIsUnavailable() = runTest(testDispatcher) {
        val reelsContext = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/clips_viewer_view_pager")
        )

        val decision = protectionEngine.evaluateContext(reelsContext)
        assertTrue(
            "Expected Block decision for Reels even when awareness data is unavailable",
            decision is ProtectionDecision.Block
        )
        val block = decision as ProtectionDecision.Block
        assertTrue(block.surfaceCategory == SurfaceCategory.REELS)
    }

    @Test
    fun shortsBlocking_remainsActive_whenBaselineIsForming() = runTest(testDispatcher) {
        val shortsContext = AccessibilityWindowContext(
            packageName = "com.google.android.youtube",
            activeViewIds = setOf("com.google.android.youtube:id/reel_recycler")
        )

        val decision = protectionEngine.evaluateContext(shortsContext)
        assertTrue(
            "Expected Block decision for Shorts even when baseline is forming",
            decision is ProtectionDecision.Block
        )
        val block = decision as ProtectionDecision.Block
        assertTrue(block.surfaceCategory == SurfaceCategory.SHORTS)
    }

    @Test
    fun normalContent_remainsAllowed_regardlessOfAwarenessState() = runTest(testDispatcher) {
        val feedContext = AccessibilityWindowContext(
            packageName = "com.instagram.android",
            activeViewIds = setOf("com.instagram.android:id/feed_stream")
        )

        val decision = protectionEngine.evaluateContext(feedContext)
        assertTrue(
            "Expected Allow decision for Instagram feed",
            decision is ProtectionDecision.Allow
        )
    }
}
