package com.stillfeed.app.feature.settings.subscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 24: Data Export Request (Figma Node 130:172 / FLOW 06: Account / Privacy).
 */
@Composable
fun DataExportScreen(
    isExporting: Boolean,
    onPrepareExport: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = StillfeedTheme.spacing.screenMargin)
                .padding(top = StillfeedTheme.spacing.lg, bottom = StillfeedTheme.spacing.xl)
        ) {
            StillfeedScreenHeader(
                sectionLabel = "PRIVACY",
                stepCounter = "02 / 04"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Download your data.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "A copy you can keep, review, or share.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            StillfeedCard {
                Text(
                    text = "WHAT'S INCLUDED",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Complete portable archive",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "• Account identifiers & verified email\n• Monitored session durations & block history\n• Monthly awareness target goals & progression\n• Current app settings & theme choices",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            StillfeedCard {
                Text(
                    text = "PRIVACY GUARANTEE",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Zero social media capture",
                    style = StillfeedTheme.typography.titleSmall,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "STILLFEED never records messages, photos, captions, comments, or video content. Only your mindful usage time is logged.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            StillfeedPrimaryButton(
                text = if (isExporting) "Generating archive..." else "Prepare export",
                onClick = onPrepareExport,
                enabled = !isExporting
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            StillfeedSecondaryButton(
                text = "Cancel",
                onClick = onNavigateBack
            )
        }
    }
}
