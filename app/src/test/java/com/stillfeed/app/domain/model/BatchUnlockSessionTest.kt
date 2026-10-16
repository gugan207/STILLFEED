package com.stillfeed.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchUnlockSessionTest {

    @Test
    fun newSession_withPositiveAllowance_isValid() {
        val now = System.currentTimeMillis()
        val session = BatchUnlockSession(
            id = "test-1",
            packageName = "com.instagram.android",
            selectedBatchAllowance = 10,
            remainingBatchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true,
            createdAtMillis = now,
            expiresAtMillis = now + 3600_000L
        )

        assertTrue(session.isValid)
        assertFalse(session.isExpired)
        assertFalse(session.isExhausted)
        assertEquals(10, session.remainingBatchAllowance)
    }

    @Test
    fun expiredSession_isNotValid() {
        val now = System.currentTimeMillis()
        val session = BatchUnlockSession(
            id = "test-2",
            packageName = "com.instagram.android",
            selectedBatchAllowance = 5,
            remainingBatchAllowance = 5,
            activityType = "MOVE_BODY",
            verifiedActivityCompletion = true,
            createdAtMillis = now - 7200_000L,
            expiresAtMillis = now - 3600_000L
        )

        assertFalse(session.isValid)
        assertTrue(session.isExpired)
        assertFalse(session.isExhausted)
    }

    @Test
    fun zeroRemainingAllowance_isExhaustedAndNotValid() {
        val now = System.currentTimeMillis()
        val session = BatchUnlockSession(
            id = "test-3",
            packageName = "com.google.android.youtube",
            selectedBatchAllowance = 5,
            remainingBatchAllowance = 0,
            activityType = "AWARENESS_CARDS",
            verifiedActivityCompletion = true,
            createdAtMillis = now,
            expiresAtMillis = now + 3600_000L
        )

        assertFalse(session.isValid)
        assertFalse(session.isExpired)
        assertTrue(session.isExhausted)
    }

    @Test
    fun unverifiedActivity_isNotValid() {
        val now = System.currentTimeMillis()
        val session = BatchUnlockSession(
            id = "test-4",
            packageName = "com.instagram.android",
            selectedBatchAllowance = 10,
            remainingBatchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = false,
            createdAtMillis = now,
            expiresAtMillis = now + 3600_000L
        )

        assertFalse(session.isValid)
    }
}
