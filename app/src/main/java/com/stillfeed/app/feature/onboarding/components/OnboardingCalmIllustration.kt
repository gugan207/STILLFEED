package com.stillfeed.app.feature.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Calm glasses/eyes illustration extracted from Figma Onboarding
 * (Node 39:2 Screen 01 and Node 39:84 Screen 05).
 *
 * Visual composition:
 * - Two rounded rectangular frames (warm cream fill, sage green stroke)
 * - Vertical dividing bridge
 * - Two calm sage pupils
 */
@Composable
fun OnboardingCalmIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val creamColor = StillfeedTheme.colors.surface
    val sageColor = StillfeedTheme.colors.accent

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val totalW = this.size.width
            val totalH = this.size.height

            // Calculate scaled dimensions based on Figma 170x106 reference
            val lensW = totalW * 0.48f
            val lensH = totalH * 0.80f
            val cr = lensH * 0.50f
            val strokeW = 2.5.dp.toPx()

            val leftX = totalW * 0.03f
            val rightX = totalW * 0.49f
            val topY = (totalH - lensH) / 2f

            // Left lens background
            drawRoundRect(
                color = creamColor,
                topLeft = Offset(leftX, topY),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(cr, cr),
                style = Fill
            )
            // Left lens border
            drawRoundRect(
                color = sageColor,
                topLeft = Offset(leftX, topY),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(cr, cr),
                style = Stroke(width = strokeW)
            )

            // Right lens background
            drawRoundRect(
                color = creamColor,
                topLeft = Offset(rightX, topY),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(cr, cr),
                style = Fill
            )
            // Right lens border
            drawRoundRect(
                color = sageColor,
                topLeft = Offset(rightX, topY),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(cr, cr),
                style = Stroke(width = strokeW)
            )

            // Center vertical bridge
            val bridgeW = 3.5.dp.toPx()
            val bridgeH = totalH * 0.95f
            val bridgeX = (totalW - bridgeW) / 2f
            val bridgeY = (totalH - bridgeH) / 2f
            drawRoundRect(
                color = sageColor,
                topLeft = Offset(bridgeX, bridgeY),
                size = Size(bridgeW, bridgeH),
                cornerRadius = CornerRadius(bridgeW / 2, bridgeW / 2),
                style = Fill
            )

            // Calm pupils
            val dotRadius = 4.5.dp.toPx()
            val leftDotCenter = Offset(leftX + lensW * 0.55f, topY + lensH * 0.48f)
            val rightDotCenter = Offset(rightX + lensW * 0.45f, topY + lensH * 0.48f)

            drawCircle(
                color = sageColor,
                radius = dotRadius,
                center = leftDotCenter
            )
            drawCircle(
                color = sageColor,
                radius = dotRadius,
                center = rightDotCenter
            )
        }
    }
}
