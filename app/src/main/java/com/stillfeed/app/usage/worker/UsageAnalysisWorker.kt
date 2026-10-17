package com.stillfeed.app.usage.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stillfeed.app.StillfeedApplication
import com.stillfeed.app.usage.sync.UsageSyncResult
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker for periodic attention usage analysis (STEP 07 Section 6).
 *
 * BATTERY & RESOURCE OPTIMIZATIONS (STEP 07 Section 7):
 * 1. 15-minute periodic schedule (Android platform minimum for periodic background work).
 * 2. Incremental execution: queries only new events since last sync watermark.
 * 3. Does NOT keep permanent threads alive; terminates immediately after completion.
 * 4. Fails safely as a no-op if usage stats permission is revoked.
 * 5. Uses [ExistingPeriodicWorkPolicy.KEEP] to strictly prevent duplicate queued jobs.
 */
class UsageAnalysisWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? StillfeedApplication ?: return Result.failure()

        return try {
            val result = app.usageSyncManager.syncUsage()

            // Safely refresh awareness progress for current month if sync completed
            try {
                val currentMonth = app.awarenessManager.getCurrentMonthIdentifier()
                app.awarenessManager.refreshMonthlyProgress(currentMonth)
            } catch (_: Exception) {
                // Fail-safe: awareness refresh failure must not crash or fail usage sync
            }

            // Safely refresh home screen widgets with updated usage data
            try {
                app.widgetUpdateCoordinator.requestUpdate()
            } catch (_: Exception) {
                // Fail-safe: widget refresh failure must never affect worker execution
            }

            when (result) {
                is UsageSyncResult.Success -> Result.success()
                is UsageSyncResult.PermissionMissing -> {
                    // Safe no-op when user hasn't granted or has revoked permission
                    Result.success()
                }
                is UsageSyncResult.Error -> Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "com.stillfeed.app.usage_analysis_periodic"
        const val IMMEDIATE_WORK_NAME = "com.stillfeed.app.usage_analysis_immediate"

        /**
         * Enqueues the periodic background usage analysis job with duplicate protection.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<UsageAnalysisWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Enqueues an immediate one-time sync run (e.g. when app is foregrounded).
         */
        fun enqueueImmediateSync(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<UsageAnalysisWorker>().build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Cancels periodic background work.
         */
        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        }
    }
}
