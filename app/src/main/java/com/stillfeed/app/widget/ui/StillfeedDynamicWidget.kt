package com.stillfeed.app.widget.ui

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.stillfeed.app.StillfeedApplication
import com.stillfeed.app.widget.model.WidgetSizeCategory
import com.stillfeed.app.widget.model.WidgetUiState

/**
 * Responsive STILLFEED dynamic home screen widget.
 *
 * Automatically adapts between:
 * - COMPACT (2x2 / 3x2)
 * - MEDIUM (4x2)
 * - LARGE (4x4)
 * and dynamically shifts its diurnal environment and companion mood with time of day and attention.
 */
class StillfeedDynamicWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(150.dp, 110.dp), // Compact cell
            DpSize(250.dp, 110.dp), // Medium cell
            DpSize(250.dp, 240.dp)  // Large cell
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? StillfeedApplication
        val repo = app?.widgetDataRepository
        val state: WidgetUiState = repo?.getWidgetUiState() ?: WidgetUiState()

        provideContent {
            val size = LocalSize.current
            val sizeCategory = when {
                size.height >= 220.dp -> WidgetSizeCategory.LARGE
                size.width >= 240.dp -> WidgetSizeCategory.MEDIUM
                else -> WidgetSizeCategory.COMPACT
            }

            StillfeedWidgetContent(
                context = context,
                state = state,
                sizeCategory = sizeCategory,
                widthDp = size.width.value.toInt(),
                heightDp = size.height.value.toInt()
            )
        }
    }
}
