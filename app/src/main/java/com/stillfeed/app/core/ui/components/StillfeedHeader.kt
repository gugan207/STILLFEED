package com.stillfeed.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * STILLFEED brand wordmark component.
 * Exact Figma style: Inter Bold 15sp, letter spacing 0.5sp.
 */
@Composable
fun StillfeedWordmark(
    modifier: Modifier = Modifier
) {
    Text(
        text = "STILLFEED",
        style = StillfeedTheme.typography.brandWordmark,
        color = StillfeedTheme.colors.textPrimary,
        modifier = modifier
    )
}

/**
 * Reusable Header component preserving the finalized Figma hierarchy:
 * 1. STILLFEED (Brand Wordmark)
 *    ↓
 * 2. Section label (e.g. "GET STARTED", "TODAY", "SETTINGS")
 *    ↓
 * 3. Step counter (e.g. "01 / 05", "01 / 03")
 */
@Composable
fun StillfeedHeader(
    sectionLabel: String,
    stepCounter: String? = null,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment
    ) {
        StillfeedWordmark()

        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

        Text(
            text = sectionLabel.uppercase(),
            style = StillfeedTheme.typography.label,
            color = StillfeedTheme.colors.textSecondary
        )

        if (stepCounter != null) {
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
            Text(
                text = stepCounter,
                style = StillfeedTheme.typography.stepIndicator,
                color = StillfeedTheme.colors.textSecondary
            )
        }
    }
}

/**
 * Onboarding / Screen Top Bar variant matching the Figma screen layout where
 * STILLFEED brand and Step counter share the top row and section label follows.
 */
@Composable
fun StillfeedScreenHeader(
    sectionLabel: String,
    stepCounter: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StillfeedWordmark()
            if (stepCounter != null) {
                Text(
                    text = stepCounter,
                    style = StillfeedTheme.typography.stepIndicator,
                    color = StillfeedTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

        Text(
            text = sectionLabel.uppercase(),
            style = StillfeedTheme.typography.label,
            color = StillfeedTheme.colors.textSecondary
        )
    }
}
