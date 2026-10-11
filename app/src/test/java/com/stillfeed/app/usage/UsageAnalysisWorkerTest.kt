package com.stillfeed.app.usage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.stillfeed.app.StillfeedApplication
import com.stillfeed.app.usage.worker.UsageAnalysisWorker
import kotlinx.coroutines.runBlocking
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
class UsageAnalysisWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<StillfeedApplication>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun schedulePeriodicSync_enqueuesUniquePeriodicWork() {
        UsageAnalysisWorker.schedulePeriodicSync(context)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(UsageAnalysisWorker.PERIODIC_WORK_NAME)
            .get()

        assertNotNull(workInfos)
        assertEquals(1, workInfos.size)
        val workInfo = workInfos[0]
        assertTrue(
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        )
    }

    @Test
    fun schedulePeriodicSync_duplicateCalls_retainSingleWorkRequest() {
        // First scheduling
        UsageAnalysisWorker.schedulePeriodicSync(context)

        // Duplicate second scheduling
        UsageAnalysisWorker.schedulePeriodicSync(context)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(UsageAnalysisWorker.PERIODIC_WORK_NAME)
            .get()

        assertEquals("Duplicate periodic scheduling must not create duplicate work items", 1, workInfos.size)
    }

    @Test
    fun enqueueImmediateSync_enqueuesImmediateWorkRequest() {
        UsageAnalysisWorker.enqueueImmediateSync(context)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(UsageAnalysisWorker.IMMEDIATE_WORK_NAME)
            .get()

        assertEquals(1, workInfos.size)
    }

    @Test
    fun doWork_executesSafelyAndReturnsSuccess() = runBlocking {
        val worker = TestListenableWorkerBuilder<UsageAnalysisWorker>(context).build()
        val result = worker.doWork()

        // Without permissions granted in test environment, it safely succeeds (no-op)
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
