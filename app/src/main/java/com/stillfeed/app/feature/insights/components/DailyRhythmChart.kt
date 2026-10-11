package com.stillfeed.app.feature.insights.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.feature.insights.DayRhythmItem

/**
 * Calm, 7-day Daily Rhythm bar visualization (Figma Node 70:124-70:138 & 130:29-130:44).
 *
 * SPECIFICATIONS FROM FIGMA:
 * - 7 bars: Monday to Sunday
 * - Bar width: 22dp with 4dp corner radius
 * - Maximum chart track height: 80dp
 * - Day letters below bars: 10sp SemiBold ("M", "T", "W", "T", "F", "S", "S")
 * - Strictly zero donut charts, score rings, or gamification colors.
 * - Meaningful text semantics for accessibility.
 */
@Composable
fun DailyRhythmChart(
    items: List<DayRhythmItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "DAILY RHYTHM",
            style = StillfeedTheme.typography.label,
            color = StillfeedTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { item ->
                val barAccessibilityText = "${item.fullDateString}, ${item.dayLabel}: ${AwarenessViewModel.formatMinutes(item.usageMinutes)}" +
                        if (item.isToday) " (Today)" else ""

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = barAccessibilityText }
                ) {
                    // Track Container (80dp max height)
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(80.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Background track (Figma Node 130:31-130:41: #D9D4CA with r=8)
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(StillfeedTheme.colors.divider)
                        )

                        // Filled usage bar (Figma Node 70:125-70:138: #7D927C Sage, today #E8C86B Gold with r=8)
                        val barColor = if (item.isToday) {
                            com.stillfeed.app.core.ui.theme.StillfeedHighlightGold
                        } else if (item.usageMinutes > 0) {
                            com.stillfeed.app.core.ui.theme.StillfeedSageGreen
                        } else {
                            StillfeedTheme.colors.divider
                        }

                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight(fraction = item.normalizedHeightFraction.coerceIn(0.08f, 1.0f))
                                .clip(RoundedCornerShape(8.dp))
                                .background(barColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                    // Day letter ("M", "T", "W", "T", "F", "S", "S")
                    Text(
                        text = item.dayLabel,
                        style = if (item.isToday) StillfeedTheme.typography.labelSmall else StillfeedTheme.typography.caption,
                        color = if (item.isToday) StillfeedTheme.colors.textPrimary else StillfeedTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
