package com.stillfeed.app.widget.ui

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.stillfeed.app.StillfeedApplication
import com.stillfeed.app.widget.model.WidgetSizeCategory
import com.stillfeed.app.widget.model.WidgetUiState

/**
 * Dedicated Large (4x4) STILLFEED Widget.
 * Matches Figma Nodes `216:81` (Evening) and `216:107` (Night).
 */
class StillfeedLargeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? StillfeedApplication
        val repo = app?.widgetDataRepository
        val state: WidgetUiState = repo?.getWidgetUiState() ?: WidgetUiState()

        provideContent {
            StillfeedWidgetContent(
                context = context,
                state = state,
                sizeCategory = WidgetSizeCategory.LARGE,
                widthDp = 360,
                heightDp = 360
            )
        }
    }
}
