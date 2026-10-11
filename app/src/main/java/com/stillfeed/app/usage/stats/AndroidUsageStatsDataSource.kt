package com.stillfeed.app.usage.stats

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process

/**
 * Production Android implementation of [UsageStatsDataSource].
 *
 * Responsibilities:
 * 1. Safely checks PACKAGE_USAGE_STATS grant state via [AppOpsManager].
 * 2. Queries system [UsageStatsManager] for chronological lifecycle events.
 * 3. Never fabricates fake usage data when permission is revoked.
 * 4. Fails safely on missing services, security exceptions, or null references.
 */
class AndroidUsageStatsDataSource(
    private val context: Context
) : UsageStatsDataSource {

    override fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }

        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun queryTimelineEvents(
        startTimeMillis: Long,
        endTimeMillis: Long
    ): List<RawUsageTimelineEvent> {
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }

        if (startTimeMillis >= endTimeMillis || startTimeMillis < 0) {
            return emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val results = mutableListOf<RawUsageTimelineEvent>()

        try {
            val usageEvents = usageStatsManager.queryEvents(startTimeMillis, endTimeMillis) ?: return emptyList()
            val event = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val pkg = event.packageName ?: continue
                results.add(
                    RawUsageTimelineEvent(
                        packageName = pkg,
                        timestampMillis = event.timeStamp,
                        eventType = event.eventType
                    )
                )
            }
        } catch (_: SecurityException) {
            // Permission revoked concurrently — return empty safely
            return emptyList()
        } catch (_: Exception) {
            // Fail-safe
            return emptyList()
        }

        return results
    }
}
