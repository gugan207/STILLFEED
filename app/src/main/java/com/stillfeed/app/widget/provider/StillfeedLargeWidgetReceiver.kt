package com.stillfeed.app.widget.provider

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.stillfeed.app.widget.ui.StillfeedLargeWidget

/**
 * Android AppWidgetReceiver for the large STILLFEED widget (Figma Nodes 216:81, 216:107).
 */
class StillfeedLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StillfeedLargeWidget()
}
