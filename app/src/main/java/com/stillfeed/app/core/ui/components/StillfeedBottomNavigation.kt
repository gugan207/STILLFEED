package com.stillfeed.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

enum class StillfeedNavDestination(val label: String) {
    Home("Home"),
    Insights("Insights"),
    Settings("Settings")
}

/**
 * STILLFEED reusable bottom navigation component.
 * Exact Figma design:
 * - 3 equal touch zones (Home, Insights, Settings)
 * - 1dp top divider border
 * - 20dp icons
 * - Active: Inter 600 10sp with primary fill
 * - Inactive: Inter 400 10sp with muted fill
 */
@Composable
fun StillfeedBottomNavigation(
    selectedDestination: StillfeedNavDestination,
    onDestinationSelected: (StillfeedNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StillfeedTheme.colors.backgroundPrimary)
            .navigationBarsPadding()
    ) {
        // Top 1dp divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(StillfeedTheme.spacing.dividerThickness)
                .background(StillfeedTheme.colors.divider)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(StillfeedTheme.spacing.navHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StillfeedNavDestination.values().forEach { destination ->
                val isSelected = destination == selectedDestination
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onDestinationSelected(destination) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NavDestinationIcon(
                        destination = destination,
                        isSelected = isSelected,
                        color = if (isSelected) StillfeedTheme.colors.textPrimary else StillfeedTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = destination.label,
                        style = if (isSelected) StillfeedTheme.typography.labelSmall else StillfeedTheme.typography.caption,
                        color = if (isSelected) StillfeedTheme.colors.textPrimary else StillfeedTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun NavDestinationIcon(
    destination: StillfeedNavDestination,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(StillfeedTheme.spacing.navIconSize)
    ) {
        val w = size.width
        val h = size.height

        when (destination) {
            StillfeedNavDestination.Home -> {
                // Home icon: roof triangle + house body
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    lineTo(w * 0.88f, h * 0.48f)
                    lineTo(w * 0.78f, h * 0.48f)
                    lineTo(w * 0.78f, h * 0.85f)
                    lineTo(w * 0.22f, h * 0.85f)
                    lineTo(w * 0.22f, h * 0.48f)
                    lineTo(w * 0.12f, h * 0.48f)
                    close()
                }
                if (isSelected) {
                    drawPath(path, color = color, style = Fill)
                } else {
                    drawPath(path, color = color, style = Stroke(width = 1.8f))
                }
            }
            StillfeedNavDestination.Insights -> {
                // 3 trend bars (bar chart)
                val barW = w * 0.2f
                val cornerRadius = CornerRadius(barW * 0.4f, barW * 0.4f)
                val strokeW = 1.5f

                // Bar 1 (left, mid height)
                val bar1Rect = Offset(w * 0.15f, h * 0.45f)
                val bar1Size = Size(barW, h * 0.42f)

                // Bar 2 (center, tall)
                val bar2Rect = Offset(w * 0.40f, h * 0.22f)
                val bar2Size = Size(barW, h * 0.65f)

                // Bar 3 (right, small)
                val bar3Rect = Offset(w * 0.65f, h * 0.35f)
                val bar3Size = Size(barW, h * 0.52f)

                if (isSelected) {
                    drawRoundRect(color, bar1Rect, bar1Size, cornerRadius, Fill)
                    drawRoundRect(color, bar2Rect, bar2Size, cornerRadius, Fill)
                    drawRoundRect(color, bar3Rect, bar3Size, cornerRadius, Fill)
                } else {
                    drawRoundRect(color, bar1Rect, bar1Size, cornerRadius, Stroke(strokeW))
                    drawRoundRect(color, bar2Rect, bar2Size, cornerRadius, Stroke(strokeW))
                    drawRoundRect(color, bar3Rect, bar3Size, cornerRadius, Stroke(strokeW))
                }
            }
            StillfeedNavDestination.Settings -> {
                // Sliders / settings gears: 3 horizontal control lines with sliders
                val strokeW = 1.6f
                val lineY1 = h * 0.30f
                val lineY2 = h * 0.52f
                val lineY3 = h * 0.74f

                // Lines
                drawLine(color, Offset(w * 0.15f, lineY1), Offset(w * 0.85f, lineY1), strokeWidth = strokeW)
                drawLine(color, Offset(w * 0.15f, lineY2), Offset(w * 0.85f, lineY2), strokeWidth = strokeW)
                drawLine(color, Offset(w * 0.15f, lineY3), Offset(w * 0.85f, lineY3), strokeWidth = strokeW)

                // Knob circles
                val r = 2.4f
                val knob1 = Offset(w * 0.38f, lineY1)
                val knob2 = Offset(w * 0.62f, lineY2)
                val knob3 = Offset(w * 0.32f, lineY3)

                if (isSelected) {
                    drawCircle(color, r * 1.3f, knob1, style = Fill)
                    drawCircle(color, r * 1.3f, knob2, style = Fill)
                    drawCircle(color, r * 1.3f, knob3, style = Fill)
                } else {
                    drawCircle(color, r * 1.2f, knob1, style = Fill)
                    drawCircle(color, r * 1.2f, knob2, style = Fill)
                    drawCircle(color, r * 1.2f, knob3, style = Fill)
                }
            }
        }
    }
}
