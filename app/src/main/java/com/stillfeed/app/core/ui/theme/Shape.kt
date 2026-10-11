package com.stillfeed.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * STILLFEED Shape Tokens.
 * Extracted directly from Figma corner radii across screens and widgets.
 */
@Immutable
data class StillfeedShapes(
    val button: RoundedCornerShape = RoundedCornerShape(28.dp),
    val card: RoundedCornerShape = RoundedCornerShape(26.dp),
    val cardSmall: RoundedCornerShape = RoundedCornerShape(22.dp),
    val badge: RoundedCornerShape = RoundedCornerShape(16.dp),
    val tag: RoundedCornerShape = RoundedCornerShape(14.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(28.dp),
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    val screen: RoundedCornerShape = RoundedCornerShape(28.dp)
)

val LocalStillfeedShapes = staticCompositionLocalOf { StillfeedShapes() }
