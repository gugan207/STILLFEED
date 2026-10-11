package com.stillfeed.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * STILLFEED Primary Button.
 * Pill-shaped (28dp radius), 56dp height, solid dark fill with light text.
 */
@Composable
fun StillfeedPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = StillfeedTheme.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = StillfeedTheme.colors.buttonPrimaryBackground,
            contentColor = StillfeedTheme.colors.buttonPrimaryText,
            disabledContainerColor = StillfeedTheme.colors.buttonPrimaryBackground.copy(alpha = 0.4f),
            disabledContentColor = StillfeedTheme.colors.buttonPrimaryText.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(StillfeedTheme.spacing.buttonHeight)
    ) {
        Text(
            text = text,
            style = StillfeedTheme.typography.button
        )
    }
}

/**
 * STILLFEED Secondary Button.
 * Pill-shaped (28dp radius), 56dp height, surface cream fill with subtle 1dp border and dark text.
 */
@Composable
fun StillfeedSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = StillfeedTheme.shapes.button,
        border = BorderStroke(
            width = StillfeedTheme.spacing.dividerThickness,
            color = StillfeedTheme.colors.buttonSecondaryBorder
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = StillfeedTheme.colors.buttonSecondaryBackground,
            contentColor = StillfeedTheme.colors.buttonSecondaryText,
            disabledContainerColor = StillfeedTheme.colors.buttonSecondaryBackground.copy(alpha = 0.5f),
            disabledContentColor = StillfeedTheme.colors.buttonSecondaryText.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(StillfeedTheme.spacing.buttonHeight)
    ) {
        Text(
            text = text,
            style = StillfeedTheme.typography.button
        )
    }
}

/**
 * STILLFEED Text Button.
 * For low-emphasis actions and navigation hints.
 */
@Composable
fun StillfeedTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = StillfeedTheme.shapes.pill,
        colors = ButtonDefaults.textButtonColors(
            contentColor = StillfeedTheme.colors.textSecondary
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = StillfeedTheme.typography.button
        )
    }
}
