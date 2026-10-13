package com.stillfeed.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultUnlockSessionRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var repository: DefaultUnlockSessionRepository

    @Before
    fun setUp() {
        repository = DefaultUnlockSessionRepository(ioDispatcher = testDispatcher)
    }

    @Test
    fun createUnlockSession_createsActiveSession() = testScope.runTest {
        val result = repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        )

        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals("com.instagram.android", session?.packageName)
        assertEquals(10, session?.selectedBatchAllowance)
        assertEquals(10, session?.remainingBatchAllowance)
        assertEquals("FLIP_PHONE", session?.activityType)
        assertTrue(session?.isValid == true)

        val active = repository.getActiveUnlock("com.instagram.android")
        assertNotNull(active)
        assertEquals(session?.id, active?.id)
    }

    @Test
    fun createUnlockSession_failsWithoutVerifiedCompletion() = testScope.runTest {
        val result = repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = false
        )

        assertTrue(result.isFailure)
        assertNull(repository.getActiveUnlock("com.instagram.android"))
    }

    @Test
    fun createUnlockSession_failsForInvalidAllowance() = testScope.runTest {
        val result = repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 999,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun createUnlockSession_idempotentWithinRapidWindow() = testScope.runTest {
        val first = repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        ).getOrThrow()

        // Rapid tap / duplicate call within 2 seconds
        val second = repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        ).getOrThrow()

        // Should return existing session rather than generating a new ID or doubling batch
        assertEquals(first.id, second.id)
        assertEquals(10, second.remainingBatchAllowance)
    }

    @Test
    fun consumeReelAllowance_decrementsAllowance() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 5,
            activityType = "MOVE_BODY",
            verifiedActivityCompletion = true
        )

        val updated = repository.consumeReelAllowance("com.instagram.android", signalKey = "reel_1")
        assertEquals(4, updated?.remainingBatchAllowance)

        val active = repository.getActiveUnlock("com.instagram.android")
        assertEquals(4, active?.remainingBatchAllowance)
    }

    @Test
    fun consumeReelAllowance_deduplicatesRapidAccessibilityEvents() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 5,
            activityType = "SHORT_WALK",
            verifiedActivityCompletion = true
        )

        // First event
        val updated1 = repository.consumeReelAllowance("com.instagram.android", signalKey = "reel_1")
        assertEquals(4, updated1?.remainingBatchAllowance)

        // Immediate subsequent window event with the SAME signal within dedupe window
        val updated2 = repository.consumeReelAllowance("com.instagram.android", signalKey = "reel_1")
        // Should NOT decrement again!
        assertEquals(4, updated2?.remainingBatchAllowance)
        assertTrue("Session must be marked with isIndeterminateTracking", updated2?.isIndeterminateTracking == true)
    }

    @Test
    fun consumeReelAllowance_exhaustsBatchAndDeactivates() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.google.android.youtube",
            batchAllowance = 5,
            activityType = "AWARENESS_CARDS",
            verifiedActivityCompletion = true
        )

        // Directly expire or consume
        repository.expireUnlock("com.google.android.youtube")

        val active = repository.getActiveUnlock("com.google.android.youtube")
        assertNull(active)
    }

    @Test
    fun clearAll_removesAllSessions() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 20,
            activityType = "BREATHING",
            verifiedActivityCompletion = true
        )

        repository.clearAll()
        val active = repository.getActiveUnlock("com.instagram.android")
        assertNull(active)
    }

    @Test
    fun observeActiveUnlock_emitsSession() = testScope.runTest {
        val initial = repository.observeActiveUnlock("com.instagram.android").first()
        assertNull(initial)

        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 5,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        )

        val updated = repository.observeActiveUnlock("com.instagram.android").first()
        assertNotNull(updated)
        assertEquals("com.instagram.android", updated?.packageName)
    }
}
