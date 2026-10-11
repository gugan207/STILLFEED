package com.stillfeed.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.domain.model.UnlockEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UnlockHistoryRepositoryTest {

    private lateinit var database: StillfeedDatabase
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LocalUnlockHistoryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        repository = LocalUnlockHistoryRepository(database.unlockHistoryDao(), testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun recordUnlock_and_getUnlockById_succeeds() = runTest(testDispatcher) {
        val event = UnlockEvent(
            id = "unlock_ev_01",
            timestampMillis = 15_000L,
            allowanceMinutes = 5,
            reason = "manual_request",
            relatedUsageEventId = "evt_insta_1"
        )
        repository.recordUnlock(event)

        val retrieved = repository.getUnlockById("unlock_ev_01")
        assertNotNull(retrieved)
        assertEquals("unlock_ev_01", retrieved?.id)
        assertEquals(5, retrieved?.allowanceMinutes)
        assertEquals("manual_request", retrieved?.reason)
        assertEquals("evt_insta_1", retrieved?.relatedUsageEventId)
    }

    @Test
    fun observeUnlocksInRange_queriesDateRange() = runTest(testDispatcher) {
        val unlock1 = UnlockEvent(
            id = "unlock_1",
            timestampMillis = 1000L,
            allowanceMinutes = 5,
            reason = "break_1"
        )
        val unlock2 = UnlockEvent(
            id = "unlock_2",
            timestampMillis = 5000L,
            allowanceMinutes = 10,
            reason = "emergency_call"
        )
        val unlock3 = UnlockEvent(
            id = "unlock_3",
            timestampMillis = 10000L,
            allowanceMinutes = 5,
            reason = "break_2"
        )

        repository.recordUnlock(unlock1)
        repository.recordUnlock(unlock2)
        repository.recordUnlock(unlock3)

        val inRange = repository.getUnlocksInRange(4000L, 8000L)
        assertEquals(1, inRange.size)
        assertEquals("unlock_2", inRange[0].id)

        val observed = repository.observeUnlocksInRange(500L, 12000L).first()
        assertEquals(3, observed.size)
    }
}
