package com.stillfeed.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.entity.AwarenessMonthlyEntity
import com.stillfeed.app.data.local.entity.DeviceBindingEntity
import com.stillfeed.app.data.local.entity.ProtectionStateEntity
import com.stillfeed.app.data.local.entity.UnlockEventEntity
import com.stillfeed.app.data.local.entity.UsageEventEntity
import com.stillfeed.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDaoTest {

    private lateinit var database: StillfeedDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun userDao_insertAndRead_returnsInsertedUser() = runTest {
        val user = UserEntity(
            id = "user_test_01",
            email = "user@example.com",
            displayName = "Test User",
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L,
            deviceBindingId = "dev_123"
        )
        database.userDao().insertUser(user)

        val retrieved = database.userDao().getUserById("user_test_01")
        assertNotNull(retrieved)
        assertEquals("user@example.com", retrieved?.email)
        assertEquals("Test User", retrieved?.displayName)
        assertEquals("dev_123", retrieved?.deviceBindingId)

        val observed = database.userDao().observeUserById("user_test_01").first()
        assertEquals("user_test_01", observed?.id)
    }

    @Test
    fun protectionStateDao_insertAndObserve_updatesCorrectly() = runTest {
        val initial = ProtectionStateEntity(
            isEnabled = false,
            updatedAtMillis = 1000L
        )
        database.protectionStateDao().insertOrUpdate(initial)
        val state1 = database.protectionStateDao().getProtectionState()
        assertEquals(false, state1?.isEnabled)

        val updated = ProtectionStateEntity(
            isEnabled = true,
            updatedAtMillis = 2000L
        )
        database.protectionStateDao().insertOrUpdate(updated)
        val state2 = database.protectionStateDao().observeProtectionState().first()
        assertEquals(true, state2?.isEnabled)
        assertEquals(2000L, state2?.updatedAtMillis)
    }

    @Test
    fun usageEventDao_insertAndRangeQuery_filtersByTimestamp() = runTest {
        val event1 = UsageEventEntity(
            id = "evt_1",
            timestampMillis = 1000L,
            packageName = "com.instagram.android",
            surfaceCategory = "REELS",
            eventType = "BLOCKED",
            durationMillis = 30000L,
            isBlocked = true,
            isUnlocked = false,
            metadata = null
        )
        val event2 = UsageEventEntity(
            id = "evt_2",
            timestampMillis = 5000L,
            packageName = "com.google.android.youtube",
            surfaceCategory = "SHORTS",
            eventType = "BLOCKED",
            durationMillis = 45000L,
            isBlocked = true,
            isUnlocked = false,
            metadata = null
        )
        val event3 = UsageEventEntity(
            id = "evt_3",
            timestampMillis = 10000L,
            packageName = "com.instagram.android",
            surfaceCategory = "FEED",
            eventType = "SESSION_END",
            durationMillis = 10000L,
            isBlocked = false,
            isUnlocked = false,
            metadata = null
        )

        database.usageEventDao().insertEvents(listOf(event1, event2, event3))
        assertEquals(3, database.usageEventDao().getEventCount())

        val inRange = database.usageEventDao().getEventsInRange(2000L, 8000L)
        assertEquals(1, inRange.size)
        assertEquals("evt_2", inRange[0].id)
    }

    @Test
    fun awarenessMonthlyDao_insertAndQuery_maintainsSeparateMonths() = runTest {
        val month1 = AwarenessMonthlyEntity(
            monthIdentifier = "2026-09",
            targetReductionPercentage = 10,
            baselineMinutes = 600,
            currentProgressMinutes = 200,
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L
        )
        val month2 = AwarenessMonthlyEntity(
            monthIdentifier = "2026-10",
            targetReductionPercentage = 15,
            baselineMinutes = 540,
            currentProgressMinutes = 50,
            targetUsageMinutes = 459,
            reductionAmountMinutes = 81,
            status = "REDUCING",
            createdAtMillis = 2000L,
            updatedAtMillis = 2000L
        )

        database.awarenessMonthlyDao().insertOrUpdate(month1)
        database.awarenessMonthlyDao().insertOrUpdate(month2)

        val retrieved1 = database.awarenessMonthlyDao().getMonthlyState("2026-09")
        val retrieved2 = database.awarenessMonthlyDao().getMonthlyState("2026-10")

        assertEquals(10, retrieved1?.targetReductionPercentage)
        assertEquals("BASELINE_FORMING", retrieved1?.status)
        assertEquals(15, retrieved2?.targetReductionPercentage)
        assertEquals(540, retrieved2?.baselineMinutes)
        assertEquals(459, retrieved2?.targetUsageMinutes)
        assertEquals(81, retrieved2?.reductionAmountMinutes)
        assertEquals("REDUCING", retrieved2?.status)
    }

    @Test
    fun unlockHistoryDao_insertAndQuery_returnsOrderedUnlocks() = runTest {
        val unlock1 = UnlockEventEntity(
            id = "unlock_1",
            timestampMillis = 1000L,
            allowanceMinutes = 5,
            reason = "manual_request",
            relatedUsageEventId = "evt_1"
        )
        val unlock2 = UnlockEventEntity(
            id = "unlock_2",
            timestampMillis = 3000L,
            allowanceMinutes = 10,
            reason = "emergency",
            relatedUsageEventId = null
        )

        database.unlockHistoryDao().insertUnlockEvent(unlock1)
        database.unlockHistoryDao().insertUnlockEvent(unlock2)

        val results = database.unlockHistoryDao().getUnlockEventsInRange(500L, 4000L)
        assertEquals(2, results.size)
        assertEquals("unlock_1", results[0].id)
        assertEquals("unlock_2", results[1].id)
    }

    @Test
    fun deviceBindingDao_insertAndClear_behavesDeterministically() = runTest {
        val binding = DeviceBindingEntity(
            bindingId = "binding_uuid_456",
            status = "BOUND",
            createdAtMillis = 1000L,
            lastVerifiedAtMillis = 1000L,
            appVersion = "1.0.0"
        )
        database.deviceBindingDao().insertOrUpdate(binding)

        val stored = database.deviceBindingDao().getDeviceBinding()
        assertNotNull(stored)
        assertEquals("binding_uuid_456", stored?.bindingId)
        assertEquals("BOUND", stored?.status)

        database.deviceBindingDao().clearDeviceBinding()
        assertNull(database.deviceBindingDao().getDeviceBinding())
    }
}
