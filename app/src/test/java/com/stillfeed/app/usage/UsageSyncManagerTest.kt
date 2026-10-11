package com.stillfeed.app.usage

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.preferences.DataStoreAppPreferencesRepository
import com.stillfeed.app.data.repository.LocalUsageRepository
import com.stillfeed.app.usage.analysis.UsageSessionAnalyzer
import com.stillfeed.app.usage.reconciliation.UsageReconciler
import com.stillfeed.app.usage.stats.RawUsageTimelineEvent
import com.stillfeed.app.usage.stats.UsageStatsDataSource
import com.stillfeed.app.usage.sync.UsageSyncManager
import com.stillfeed.app.usage.sync.UsageSyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
class UsageSyncManagerTest {

    private lateinit var database: StillfeedDatabase
    private lateinit var usageRepository: LocalUsageRepository
    private lateinit var preferencesRepo: DataStoreAppPreferencesRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var dataStoreScope: CoroutineScope

    class FakeUsageStatsDataSource : UsageStatsDataSource {
        var hasPermission: Boolean = true
        var eventsToReturn: List<RawUsageTimelineEvent> = emptyList()
        var lastQueriedStart: Long? = null
        var lastQueriedEnd: Long? = null

        override fun hasUsageStatsPermission(): Boolean = hasPermission

        override fun queryTimelineEvents(startTimeMillis: Long, endTimeMillis: Long): List<RawUsageTimelineEvent> {
            lastQueriedStart = startTimeMillis
            lastQueriedEnd = endTimeMillis
            return eventsToReturn
        }
    }

    private lateinit var fakeDataSource: FakeUsageStatsDataSource
    private lateinit var syncManager: UsageSyncManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        usageRepository = LocalUsageRepository(database.usageEventDao(), testDispatcher)

        dataStoreScope = CoroutineScope(testDispatcher + Job())
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { context.preferencesDataStoreFile("test_sync_prefs_${UUID.randomUUID()}") }
        )
        preferencesRepo = DataStoreAppPreferencesRepository(testDataStore)

        fakeDataSource = FakeUsageStatsDataSource()
        val reconciler = UsageReconciler(usageRepository)
        syncManager = UsageSyncManager(
            usageStatsDataSource = fakeDataSource,
            sessionAnalyzer = UsageSessionAnalyzer(),
            reconciler = reconciler,
            preferencesRepository = preferencesRepo
        )
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        database.close()
    }

    @Test
    fun syncUsage_whenPermissionMissing_returnsPermissionMissingWithoutCrashing() = testScope.runTest {
        fakeDataSource.hasPermission = false

        val result = syncManager.syncUsage()

        assertEquals(UsageSyncResult.PermissionMissing, result)
        assertEquals(0, usageRepository.getEventCount())
    }

    @Test
    fun syncUsage_whenEmptyEvents_advancesWatermarkAndReturnsSuccess() = testScope.runTest {
        fakeDataSource.hasPermission = true
        fakeDataSource.eventsToReturn = emptyList()

        val now = 10_000_000L
        val result = syncManager.syncUsage(now)

        assertTrue(result is UsageSyncResult.Success)
        val success = result as UsageSyncResult.Success
        assertEquals(0, success.newSessionsCount)
        assertEquals(0L, success.syncedDurationMillis)

        // Watermark advanced
        val prefs = preferencesRepo.getPreferences()
        assertEquals(now, prefs.lastUsageSyncTimestampMillis)
    }

    @Test
    fun syncUsage_withValidEvents_persistsSessionsAndUpdatesWatermark() = testScope.runTest {
        fakeDataSource.hasPermission = true
        val start = 1_000_000L
        val end = 1_030_000L // 30s session
        fakeDataSource.eventsToReturn = listOf(
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, start, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, end, UsageSessionAnalyzer.EVENT_TYPE_PAUSED)
        )

        val now = end + 5000L
        val result = syncManager.syncUsage(now)

        assertTrue(result is UsageSyncResult.Success)
        val success = result as UsageSyncResult.Success
        assertEquals(1, success.newSessionsCount)
        assertEquals(30_000L, success.syncedDurationMillis)

        // Verify database entry
        assertEquals(1, usageRepository.getEventCount())

        // Verify watermark
        val prefs = preferencesRepo.getPreferences()
        assertEquals(now, prefs.lastUsageSyncTimestampMillis)
    }

    @Test
    fun subsequentSyncRuns_doNotDuplicatePreviouslySyncedEvents() = testScope.runTest {
        fakeDataSource.hasPermission = true
        val start = 1_000_000L
        val end = 1_030_000L
        fakeDataSource.eventsToReturn = listOf(
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, start, UsageSessionAnalyzer.EVENT_TYPE_RESUMED),
            RawUsageTimelineEvent(UsageSessionAnalyzer.PACKAGE_INSTAGRAM, end, UsageSessionAnalyzer.EVENT_TYPE_PAUSED)
        )

        val run1Time = end + 1000L
        val result1 = syncManager.syncUsage(run1Time)
        assertTrue(result1 is UsageSyncResult.Success)
        assertEquals(1, (result1 as UsageSyncResult.Success).newSessionsCount)

        // Second run with identical events returned by mock
        val run2Time = end + 2000L
        val result2 = syncManager.syncUsage(run2Time)
        assertTrue(result2 is UsageSyncResult.Success)
        assertEquals(0, (result2 as UsageSyncResult.Success).newSessionsCount)

        assertEquals("Room must retain exactly 1 session", 1, usageRepository.getEventCount())
    }
}
