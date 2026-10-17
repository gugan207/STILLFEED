package com.stillfeed.app.widget.update

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.stillfeed.app.widget.ui.StillfeedCompactWidget
import com.stillfeed.app.widget.ui.StillfeedDynamicWidget
import com.stillfeed.app.widget.ui.StillfeedLargeWidget
import com.stillfeed.app.widget.ui.StillfeedMediumWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates efficient, battery-conscious updates across all active STILLFEED home screen widgets.
 *
 * Capabilities:
 * - Refreshes all widget variants (Dynamic, Compact, Medium, Large).
 * - Debounces rapid consecutive updates to respect Android background limits.
 * - Called by UsageSyncManager, ProtectionEngine, and Preferences changes.
 */
class WidgetUpdateCoordinator(
    private val context: Context
) {
    private val updateMutex = Mutex()
    private var lastUpdateMillis = 0L

    /**
     * Immediately updates all widgets asynchronously using IO dispatcher.
     */
    fun requestUpdate(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        scope.launch {
            updateMutex.withLock {
                val now = System.currentTimeMillis()
                // Minimum 1.5s debounce to protect against high-frequency updates
                if (now - lastUpdateMillis >= 1500L) {
                    lastUpdateMillis = now
                    updateAllWidgets()
                }
            }
        }
    }

    /**
     * Suspends and updates all active widget instances across providers.
     */
    suspend fun updateAllWidgets() {
        runCatching { StillfeedDynamicWidget().updateAll(context) }
        runCatching { StillfeedCompactWidget().updateAll(context) }
        runCatching { StillfeedMediumWidget().updateAll(context) }
        runCatching { StillfeedLargeWidget().updateAll(context) }
    }
}
