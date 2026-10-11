package com.stillfeed.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * STILLFEED Divider line.
 * Default 1dp height with theme divider color.
 */
@Composable
fun StillfeedDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = StillfeedTheme.spacing.dividerThickness,
    color: Color = StillfeedTheme.colors.divider
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}
