package com.stillfeed.app.widget.provider

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.stillfeed.app.widget.ui.StillfeedMediumWidget

/**
 * Android AppWidgetReceiver for the medium STILLFEED widget (Figma Nodes 216:33, 216:58).
 */
class StillfeedMediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StillfeedMediumWidget()
}
