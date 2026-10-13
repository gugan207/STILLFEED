package com.stillfeed.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.data.local.StillfeedDatabase
import com.stillfeed.app.data.local.dao.ActiveUnlockSessionDao
import com.stillfeed.app.data.local.entity.ActiveUnlockSessionEntity
import com.stillfeed.app.domain.model.BatchUnlockSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DefaultUnlockSessionRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var database: StillfeedDatabase
    private lateinit var unlockHistoryRepository: LocalUnlockHistoryRepository
    private lateinit var repository: DefaultUnlockSessionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = StillfeedDatabase.buildInMemory(context)
        unlockHistoryRepository = LocalUnlockHistoryRepository(database.unlockHistoryDao(), testDispatcher)
        repository = DefaultUnlockSessionRepository(
            activeUnlockSessionDao = database.activeUnlockSessionDao(),
            unlockHistoryRepository = unlockHistoryRepository,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createUnlockSession_createsActiveSession_andPersistsToRoom() = testScope.runTest {
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

        // Verify active in memory
        val active = repository.getActiveUnlock("com.instagram.android")
        assertNotNull(active)
        assertEquals(session?.id, active?.id)

        // Verify durable Room persistence
        val persisted = database.activeUnlockSessionDao().getSessionByPackage("com.instagram.android")
        assertNotNull("Session must be stored in Room", persisted)
        assertEquals(session?.id, persisted?.id)
        assertEquals(10, persisted?.remainingBatchAllowance)
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
        assertNull(database.activeUnlockSessionDao().getSessionByPackage("com.instagram.android"))
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
    fun createUnlockSession_fails_whenPersistenceFails() = testScope.runTest {
        val failingDao = object : ActiveUnlockSessionDao {
            override suspend fun insertOrUpdate(session: ActiveUnlockSessionEntity) {
                throw java.sql.SQLException("Disk write failure: Room storage unwriteable")
            }
            override fun insertOrUpdateSync(session: ActiveUnlockSessionEntity) {
                throw java.sql.SQLException("Disk write failure: Room storage unwriteable")
            }
            override suspend fun getSessionByPackage(packageName: String): ActiveUnlockSessionEntity? = null
            override fun getSessionByPackageSync(packageName: String): ActiveUnlockSessionEntity? = null
            override suspend fun getAllSessions(): List<ActiveUnlockSessionEntity> = emptyList()
            override fun getAllSessionsSync(): List<ActiveUnlockSessionEntity> = emptyList()
            override fun observeSessionByPackage(packageName: String) = kotlinx.coroutines.flow.emptyFlow<ActiveUnlockSessionEntity?>()
            override suspend fun deleteSessionByPackage(packageName: String): Int = 0
            override fun deleteSessionByPackageSync(packageName: String): Int = 0
            override suspend fun deleteAllSessions(): Int = 0
            override fun deleteAllSessionsSync(): Int = 0
        }

        val failingRepo = DefaultUnlockSessionRepository(
            activeUnlockSessionDao = failingDao,
            unlockHistoryRepository = unlockHistoryRepository,
            ioDispatcher = testDispatcher
        )

        val result = failingRepo.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        )

        // If persistence fails, never report an unlock as durably saved
        assertTrue("Must report failure when persistence fails", result.isFailure)
        assertNull(failingRepo.getActiveUnlock("com.instagram.android"))
    }

    @Test
    fun createUnlockSession_idempotentWithinRapidWindow() = testScope.runTest {
        val first = repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        ).getOrThrow()

        // Rapid duplicate call within 2 seconds
        val second = repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        ).getOrThrow()

        assertEquals(first.id, second.id)
        assertEquals(10, second.remainingBatchAllowance)
    }

    @Test
    fun createUnlockSession_recordsAuditHistoryWithZeroAllowanceMinutes() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        )

        // Check history in Room
        val historyEvents = database.unlockHistoryDao().getUnlockEventsInRange(0L, Long.MAX_VALUE)
        assertEquals(1, historyEvents.size)
        // Must NOT store Reel count as minutes in allowanceMinutes field!
        assertEquals(0, historyEvents[0].allowanceMinutes)
        assertTrue(historyEvents[0].reason.contains("10 reels"))
    }

    @Test
    fun consumeReelAllowance_decrementsAllowanceOnFirstEntry() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 5,
            activityType = "MOVE_BODY",
            verifiedActivityCompletion = true
        )

        val updated = repository.consumeReelAllowance(
            packageName = "com.instagram.android",
            signalKey = "clips_viewer_view_pager",
            isExplicitTransition = false
        )
        assertEquals(4, updated?.remainingBatchAllowance)

        val active = repository.getActiveUnlock("com.instagram.android")
        assertEquals(4, active?.remainingBatchAllowance)

        // Room entity also updated
        val persisted = database.activeUnlockSessionDao().getSessionByPackage("com.instagram.android")
        assertEquals(4, persisted?.remainingBatchAllowance)
    }

    @Test
    fun consumeReelAllowance_deduplicatesRepeatedContentChangeEventsOnSameContainer() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 5,
            activityType = "SHORT_WALK",
            verifiedActivityCompletion = true
        )

        // First event (entry into short-form viewer)
        val updated1 = repository.consumeReelAllowance(
            packageName = "com.instagram.android",
            signalKey = "clips_viewer_view_pager",
            isExplicitTransition = false
        )
        assertEquals(4, updated1?.remainingBatchAllowance)

        // Repeated content-change events on the same static container
        // (captions, playback tick, like button) without an explicit transition
        val updated2 = repository.consumeReelAllowance(
            packageName = "com.instagram.android",
            signalKey = "clips_viewer_view_pager",
            isExplicitTransition = false
        )
        assertEquals(4, updated2?.remainingBatchAllowance)
        assertTrue("Indeterminate tracking must be flagged on static container reuse", updated2?.isIndeterminateTracking == true)

        val updated3 = repository.consumeReelAllowance(
            packageName = "com.instagram.android",
            signalKey = "clips_viewer_view_pager",
            isExplicitTransition = false
        )
        assertEquals(4, updated3?.remainingBatchAllowance)
    }

    @Test
    fun consumeReelAllowance_verifiedTransition_decrementsAllowance() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 5,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        )

        // First reel
        repository.consumeReelAllowance(
            packageName = "com.instagram.android",
            signalKey = "clips_viewer_view_pager",
            isExplicitTransition = false
        )

        // Verified scroll to 2nd reel after human delay (> 1.5s)
        Thread.sleep(1600)
        val afterScroll = repository.consumeReelAllowance(
            packageName = "com.instagram.android",
            signalKey = "clips_viewer_view_pager",
            isExplicitTransition = true
        )

        assertEquals(3, afterScroll?.remainingBatchAllowance)
    }

    @Test
    fun consumeReelAllowance_rapidDuplicateTransitions_doNotExhaustBatch() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 5,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        )

        // First reel
        repository.consumeReelAllowance(
            packageName = "com.instagram.android",
            signalKey = "clips_viewer_view_pager",
            isExplicitTransition = false
        )

        // Rapid duplicate scroll events (< 1.5s apart)
        val duplicate1 = repository.consumeReelAllowance(
            packageName = "com.instagram.android",
            signalKey = "clips_viewer_view_pager",
            isExplicitTransition = true
        )
        assertEquals(4, duplicate1?.remainingBatchAllowance)

        val duplicate2 = repository.consumeReelAllowance(
            packageName = "com.instagram.android",
            signalKey = "clips_viewer_view_pager",
            isExplicitTransition = true
        )
        assertEquals(4, duplicate2?.remainingBatchAllowance)
    }

    @Test
    fun consumeReelAllowance_sameStaticContainerAcrossDifferentReels_decrementsOnTransitions() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 5,
            activityType = "BREATHING",
            verifiedActivityCompletion = true
        )

        // Reel 1: initial entry
        val r1 = repository.consumeReelAllowance("com.instagram.android", "clips_viewer_view_pager", false)
        assertEquals(4, r1?.remainingBatchAllowance)

        // Reel 2: scroll transition after human delay
        Thread.sleep(1600)
        val r2 = repository.consumeReelAllowance("com.instagram.android", "clips_viewer_view_pager", true)
        assertEquals(3, r2?.remainingBatchAllowance)

        // Reel 3: scroll transition after human delay
        Thread.sleep(1600)
        val r3 = repository.consumeReelAllowance("com.instagram.android", "clips_viewer_view_pager", true)
        assertEquals(2, r3?.remainingBatchAllowance)
    }

    @Test
    fun consumeReelAllowance_exhaustsBatchAndDeactivates() = testScope.runTest {
        repository.createUnlockSession(
            packageName = "com.google.android.youtube",
            batchAllowance = 5,
            activityType = "AWARENESS_CARDS",
            verifiedActivityCompletion = true
        )

        repository.expireUnlock("com.google.android.youtube")

        val active = repository.getActiveUnlock("com.google.android.youtube")
        assertNull(active)
        assertNull(database.activeUnlockSessionDao().getSessionByPackage("com.google.android.youtube"))
    }

    @Test
    fun repositoryReconstruction_restoresCorrectRemainingAllowance() = testScope.runTest {
        // 1. Create a session in repo instance 1
        repository.createUnlockSession(
            packageName = "com.instagram.android",
            batchAllowance = 10,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true
        )

        // Consume 1 allowance
        repository.consumeReelAllowance(
            packageName = "com.instagram.android",
            signalKey = "clips_viewer_view_pager",
            isExplicitTransition = false
        )
        assertEquals(9, repository.getActiveUnlock("com.instagram.android")?.remainingBatchAllowance)

        // 2. Simulate process death & repository reconstruction with same database
        val reconstructedRepo = DefaultUnlockSessionRepository(
            activeUnlockSessionDao = database.activeUnlockSessionDao(),
            unlockHistoryRepository = unlockHistoryRepository,
            ioDispatcher = testDispatcher
        )

        // 3. Verify that the remaining allowance of 9 was accurately restored from Room
        val restored = reconstructedRepo.getActiveUnlock("com.instagram.android")
        assertNotNull("Active session must be restored from Room", restored)
        assertEquals("com.instagram.android", restored?.packageName)
        assertEquals(10, restored?.selectedBatchAllowance)
        assertEquals(9, restored?.remainingBatchAllowance)
        assertEquals("FLIP_PHONE", restored?.activityType)
        assertTrue(restored?.isValid == true)
    }

    @Test
    fun repositoryReconstruction_discardsExpiredSessions() = testScope.runTest {
        val past = System.currentTimeMillis() - 100_000L
        val expiredEntity = ActiveUnlockSessionEntity(
            id = "expired_sess_1",
            packageName = "com.expired.app",
            selectedBatchAllowance = 10,
            remainingBatchAllowance = 8,
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true,
            createdAtMillis = past - 100_000L,
            expiresAtMillis = past, // expired
            isIndeterminateTracking = false
        )
        database.activeUnlockSessionDao().insertOrUpdate(expiredEntity)

        // Reconstruct repository
        val reconstructedRepo = DefaultUnlockSessionRepository(
            activeUnlockSessionDao = database.activeUnlockSessionDao(),
            unlockHistoryRepository = unlockHistoryRepository,
            ioDispatcher = testDispatcher
        )

        assertNull("Expired session must not resume as active", reconstructedRepo.getActiveUnlock("com.expired.app"))
        assertNull("Expired session must be cleaned up from Room", database.activeUnlockSessionDao().getSessionByPackage("com.expired.app"))
    }

    @Test
    fun repositoryReconstruction_discardsExhaustedSessions() = testScope.runTest {
        val now = System.currentTimeMillis()
        val exhaustedEntity = ActiveUnlockSessionEntity(
            id = "exhausted_sess_1",
            packageName = "com.exhausted.app",
            selectedBatchAllowance = 10,
            remainingBatchAllowance = 0, // exhausted
            activityType = "FLIP_PHONE",
            verifiedActivityCompletion = true,
            createdAtMillis = now,
            expiresAtMillis = now + 100_000L,
            isIndeterminateTracking = false
        )
        database.activeUnlockSessionDao().insertOrUpdate(exhaustedEntity)

        // Reconstruct repository
        val reconstructedRepo = DefaultUnlockSessionRepository(
            activeUnlockSessionDao = database.activeUnlockSessionDao(),
            unlockHistoryRepository = unlockHistoryRepository,
            ioDispatcher = testDispatcher
        )

        assertNull("Exhausted session must not resume as active", reconstructedRepo.getActiveUnlock("com.exhausted.app"))
        assertNull("Exhausted session must be cleaned up from Room", database.activeUnlockSessionDao().getSessionByPackage("com.exhausted.app"))
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
        assertNull(database.activeUnlockSessionDao().getSessionByPackage("com.instagram.android"))
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
