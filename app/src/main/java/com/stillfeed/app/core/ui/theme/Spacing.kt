package com.stillfeed.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * STILLFEED Spacing & Dimension Tokens.
 * Extracted directly from Figma layout grids and component frames.
 * Standard screen margin is 28dp across all screens.
 */
@Immutable
data class StillfeedSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val screenMargin: Dp = 28.dp,
    val section: Dp = 32.dp,
    val jumbo: Dp = 48.dp,
    val dividerThickness: Dp = 1.dp,
    val buttonHeight: Dp = 56.dp,
    val buttonHeightSmall: Dp = 48.dp,
    val cardPadding: Dp = 20.dp,
    val navHeight: Dp = 64.dp,
    val navIconSize: Dp = 20.dp
)

val LocalStillfeedSpacing = staticCompositionLocalOf { StillfeedSpacing() }
