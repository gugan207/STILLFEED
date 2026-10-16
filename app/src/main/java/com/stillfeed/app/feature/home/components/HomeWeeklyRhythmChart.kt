package com.stillfeed.app.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.stillfeed.app.core.ui.theme.StillfeedHighlightGold
import com.stillfeed.app.core.ui.theme.StillfeedSageGreen
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.awareness.AwarenessViewModel
import com.stillfeed.app.feature.insights.DayRhythmItem

/**
 * Calm, 7-day mini-rhythm bar chart for Home (Day) (Figma Node 39:103).
 *
 * SPECIFICATIONS FROM FIGMA:
 * - 7 bars: Monday to Sunday (M T W T F S S)
 * - Bar width: 22dp with 8dp corner radius
 * - Track height: 72dp
 * - Bar colors: #7D927C Sage Green for history, #E8C86B Gold for today
 * - Labels: 10sp Regular #6D746E
 * - Meaningful accessibility semantics.
 */
@Composable
fun HomeWeeklyRhythmChart(
    items: List<DayRhythmItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "YOUR WEEK",
            style = StillfeedTheme.typography.label,
            color = StillfeedTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
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
                    // Track Container (72dp height)
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(72.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Background track
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(StillfeedTheme.colors.divider)
                        )

                        // Filled bar
                        val barColor = if (item.isToday) {
                            StillfeedHighlightGold
                        } else if (item.usageMinutes > 0) {
                            StillfeedSageGreen
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

                    // Day letter (M, T, W, T, F, S, S)
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
