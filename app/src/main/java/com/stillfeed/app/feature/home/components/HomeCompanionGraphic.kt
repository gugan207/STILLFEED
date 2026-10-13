package com.stillfeed.app.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.theme.StillfeedHighlightGold
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Lavender color token for the evening companion moon (Figma Node 39:137).
 */
val StillfeedEveningLavender = Color(0xFFB7AEC8)

/**
 * Calm sun companion graphic for Home (Day) (Figma Node 39:103).
 *
 * SPECIFICATION FROM FIGMA:
 * - Outer circle: fill #E8C86B (Gold), diameter 36dp.
 * - Inset circle: fill #F5F0E7 (Warm Paper background), diameter 18.72dp centered.
 */
@Composable
fun DaySunCompanion(
    modifier: Modifier = Modifier
) {
    val backgroundColor = StillfeedTheme.colors.backgroundPrimary
    Canvas(
        modifier = modifier
            .size(36.dp)
            .semantics { contentDescription = "Day companion sun graphic" }
    ) {
        val outerRadius = size.minDimension / 2f
        val innerRadius = outerRadius * (18.72f / 36f)
        val centerOffset = Offset(size.width / 2f, size.height / 2f)

        // Outer sun ring in gold
        drawCircle(
            color = StillfeedHighlightGold,
            radius = outerRadius,
            center = centerOffset
        )

        // Inset circle matching the screen background
        drawCircle(
            color = backgroundColor,
            radius = innerRadius,
            center = centerOffset
        )
    }
}

/**
 * Calm crescent moon companion graphic for Home (Evening) (Figma Node 39:137).
 *
 * SPECIFICATION FROM FIGMA:
 * - Base circle: fill #B7AEC8 (Lavender), diameter 36dp at (0, 0).
 * - Offset mask circle: fill #F5F0E7 (Warm Paper background), diameter 36dp shifted (+9dp, -2.88dp).
 */
@Composable
fun EveningMoonCompanion(
    modifier: Modifier = Modifier
) {
    val backgroundColor = StillfeedTheme.colors.backgroundPrimary
    Canvas(
        modifier = modifier
            .size(36.dp)
            .semantics { contentDescription = "Evening companion crescent moon graphic" }
    ) {
        val radius = size.minDimension / 2f
        val centerOffset = Offset(radius, radius)

        // Base moon disc in lavender
        drawCircle(
            color = StillfeedEveningLavender,
            radius = radius,
            center = centerOffset
        )

        // Offset circle matching background to carve the crescent
        val xOffset = 9.dp.toPx()
        val yOffset = (-2.88).dp.toPx()
        drawCircle(
            color = backgroundColor,
            radius = radius,
            center = Offset(centerOffset.x + xOffset, centerOffset.y + yOffset)
        )
    }
}
