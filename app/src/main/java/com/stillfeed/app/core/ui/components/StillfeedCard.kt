package com.stillfeed.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * STILLFEED Card container.
 * Features 26dp rounded corners, surface cream fill, and subtle 1dp border.
 */
@Composable
fun StillfeedCard(
    modifier: Modifier = Modifier,
    shape: Shape = StillfeedTheme.shapes.card,
    contentPadding: Dp = StillfeedTheme.spacing.cardPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = StillfeedTheme.colors.surface,
        border = BorderStroke(
            width = StillfeedTheme.spacing.dividerThickness,
            color = StillfeedTheme.colors.divider
        )
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
