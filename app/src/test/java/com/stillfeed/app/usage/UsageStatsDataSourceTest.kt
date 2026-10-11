package com.stillfeed.app.usage

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.stillfeed.app.usage.stats.AndroidUsageStatsDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UsageStatsDataSourceTest {

    private lateinit var context: Context
    private lateinit var dataSource: AndroidUsageStatsDataSource
    private lateinit var appOpsManager: AppOpsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataSource = AndroidUsageStatsDataSource(context)
        appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }

    @Test
    fun permissionAvailable_returnsTrue() {
        shadowOf(appOpsManager).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_ALLOWED
        )

        assertTrue(dataSource.hasUsageStatsPermission())
    }

    @Test
    fun permissionUnavailable_returnsFalse() {
        shadowOf(appOpsManager).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_IGNORED
        )

        assertFalse(dataSource.hasUsageStatsPermission())
    }

    @Test
    fun permissionRevokedAfterPreviousSuccess_transitionsToFalse() {
        // Initially granted
        shadowOf(appOpsManager).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_ALLOWED
        )
        assertTrue(dataSource.hasUsageStatsPermission())

        // Subsequently revoked by user in settings
        shadowOf(appOpsManager).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_IGNORED
        )
        assertFalse(dataSource.hasUsageStatsPermission())
    }

    @Test
    fun queryTimelineEvents_whenPermissionDenied_returnsEmptyListWithoutCrashing() {
        shadowOf(appOpsManager).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_IGNORED
        )

        val events = dataSource.queryTimelineEvents(1000L, 2000L)
        assertTrue("Must return empty list when permission is denied", events.isEmpty())
    }

    @Test
    fun queryTimelineEvents_invalidTimeBounds_returnsEmptyListSafely() {
        shadowOf(appOpsManager).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_ALLOWED
        )

        // Start >= End
        val reversed = dataSource.queryTimelineEvents(5000L, 2000L)
        assertTrue(reversed.isEmpty())

        // Negative start
        val negative = dataSource.queryTimelineEvents(-100L, 2000L)
        assertTrue(negative.isEmpty())
    }
}
