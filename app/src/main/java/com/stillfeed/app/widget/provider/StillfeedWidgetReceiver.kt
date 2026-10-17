package com.stillfeed.app.widget.provider

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.stillfeed.app.widget.ui.StillfeedDynamicWidget

/**
 * Android AppWidgetReceiver for the dynamic responsive STILLFEED widget.
 */
class StillfeedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StillfeedDynamicWidget()
}
