package com.stillfeed.app.widget.ui

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.stillfeed.app.StillfeedApplication
import com.stillfeed.app.widget.model.WidgetSizeCategory
import com.stillfeed.app.widget.model.WidgetUiState

/**
 * Dedicated Medium (4x2) STILLFEED Widget.
 * Matches Figma Nodes `216:33` (Day) and `216:58` (Afternoon).
 */
class StillfeedMediumWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? StillfeedApplication
        val repo = app?.widgetDataRepository
        val state: WidgetUiState = repo?.getWidgetUiState() ?: WidgetUiState()

        provideContent {
            StillfeedWidgetContent(
                context = context,
                state = state,
                sizeCategory = WidgetSizeCategory.MEDIUM,
                widthDp = 430,
                heightDp = 190
            )
        }
    }
}
