package com.stillfeed.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.domain.model.UsageEvent
import com.stillfeed.app.domain.model.UsageEventType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsageRepositoryTest {

    private lateinit var database: StillfeedDatabase
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LocalUsageRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        repository = LocalUsageRepository(database.usageEventDao(), testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUsageEvent_and_readEvent_succeeds() = runTest(testDispatcher) {
        val event = UsageEvent(
            id = "evt_insta_reels_1",
            timestampMillis = 10_000L,
            packageName = "com.instagram.android",
            surfaceCategory = SurfaceCategory.REELS,
            eventType = UsageEventType.BLOCKED,
            durationMillis = 15_000L,
            isBlocked = true,
            isUnlocked = false,
            metadata = "attempted_swipe"
        )
        repository.recordEvent(event)

        val retrieved = repository.getEventById("evt_insta_reels_1")
        assertNotNull(retrieved)
        assertEquals("evt_insta_reels_1", retrieved?.id)
        assertEquals("com.instagram.android", retrieved?.packageName)
        assertEquals(SurfaceCategory.REELS, retrieved?.surfaceCategory)
        assertEquals(UsageEventType.BLOCKED, retrieved?.eventType)
        assertEquals(15_000L, retrieved?.durationMillis)
        assertTrue(retrieved?.isBlocked == true)
    }

    @Test
    fun queryDateRange_filtersEventsCorrectly() = runTest(testDispatcher) {
        val event1 = UsageEvent(
            id = "evt_t1",
            timestampMillis = 1000L,
            packageName = "com.instagram.android",
            surfaceCategory = SurfaceCategory.REELS,
            eventType = UsageEventType.BLOCKED
        )
        val event2 = UsageEvent(
            id = "evt_t2",
            timestampMillis = 5000L,
            packageName = "com.google.android.youtube",
            surfaceCategory = SurfaceCategory.SHORTS,
            eventType = UsageEventType.CONTENT_DETECTED
        )
        val event3 = UsageEvent(
            id = "evt_t3",
            timestampMillis = 10000L,
            packageName = "com.instagram.android",
            surfaceCategory = SurfaceCategory.FEED,
            eventType = UsageEventType.SESSION_START
        )

        repository.recordEvents(listOf(event1, event2, event3))

        val results = repository.getEventsInRange(4000L, 8000L)
        assertEquals(1, results.size)
        assertEquals("evt_t2", results[0].id)
    }

    @Test
    fun multipleEventsPreservedCorrectly_andObservable() = runTest(testDispatcher) {
        val events = (1..5).map { i ->
            UsageEvent(
                id = "evt_batch_$i",
                timestampMillis = i * 1000L,
                packageName = "com.instagram.android",
                surfaceCategory = SurfaceCategory.REELS,
                eventType = UsageEventType.CONTENT_DETECTED
            )
        }
        repository.recordEvents(events)

        val totalCount = repository.getEventCount()
        assertEquals(5, totalCount)

        val observed = repository.observeEventsInRange(1000L, 5000L).first()
        assertEquals(5, observed.size)
    }
}
