package com.stillfeed.app.feature.settings.subscreens

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 28: Deletion Complete / Pending (Figma Node 147:38 Roadmap Card / FLOW 08: Deletion).
 */
@Composable
fun DeletionPendingScreen(
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                sectionLabel = "ACCOUNT",
                stepCounter = null
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Account deleted.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "Your data has been completely removed from STILLFEED.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            StillfeedCard {
                Text(
                    text = "STATUS",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Local & cloud cleanup complete",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "• All sessions have been terminated.\n• Usage records and databases have been wiped.\n• Accessibility monitoring has been deactivated.\n\nThank you for spending time with STILLFEED.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            StillfeedPrimaryButton(
                text = "Exit STILLFEED",
                onClick = {
                    onExitApp()
                    (context as? Activity)?.finishAffinity()
                }
            )
        }
    }
}
