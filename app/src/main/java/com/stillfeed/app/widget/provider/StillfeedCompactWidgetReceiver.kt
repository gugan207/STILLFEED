package com.stillfeed.app.widget.provider

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.stillfeed.app.widget.ui.StillfeedCompactWidget

/**
 * Android AppWidgetReceiver for the compact STILLFEED widget (Figma Node 216:2).
 */
class StillfeedCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StillfeedCompactWidget()
}
