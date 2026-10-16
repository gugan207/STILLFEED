package com.stillfeed.app.feature.settings.subscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 23: Privacy Center (Figma Node 130:139 / FLOW 06: Account / Privacy).
 */
@Composable
fun PrivacyCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDataExport: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
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
                stepCounter = "01 / 04"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Your privacy.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "What we store · why we need it · how long we keep it.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Section 1: Account
            PrivacyCategoryCard(
                category = "ACCOUNT",
                summary = "Name, email, session token",
                detail = "Stored securely with Supabase GoTrue authentication to restore your session. Passwords are never stored in plaintext."
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // Section 2: Reel Usage
            PrivacyCategoryCard(
                category = "REEL USAGE",
                summary = "Used for awareness + reports",
                detail = "Stored strictly on your physical device in local Room database. Never sent to advertising trackers, third parties, or brokers."
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // Section 3: Reports
            PrivacyCategoryCard(
                category = "REPORTS",
                summary = "Weekly · monthly · yearly",
                detail = "Aggregated locally on-device. No telemetry profiles are uploaded or sold."
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // Section 4: Support
            PrivacyCategoryCard(
                category = "SUPPORT",
                summary = "Feedback + diagnostics",
                detail = "Only generated when you explicitly request help via WhatsApp or email. Contains device model and permission status."
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // CTA 1: Download my data
            StillfeedSecondaryButton(
                text = "Download my data",
                onClick = onNavigateToDataExport
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // CTA 2: Delete my account
            TextButton(
                onClick = onNavigateToDeleteAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Delete my account",
                    style = StillfeedTheme.typography.bodyMedium,
                    color = StillfeedTheme.colors.destructive
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            StillfeedSecondaryButton(
                text = "Back to Settings",
                onClick = onNavigateBack
            )
        }
    }
}

@Composable
private fun PrivacyCategoryCard(
    category: String,
    summary: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    StillfeedCard(modifier = modifier) {
        Text(
            text = category,
            style = StillfeedTheme.typography.label,
            color = StillfeedTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
        Text(
            text = summary,
            style = StillfeedTheme.typography.titleSmall,
            color = StillfeedTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
        Text(
            text = detail,
            style = StillfeedTheme.typography.bodySmall,
            color = StillfeedTheme.colors.textSecondary
        )
    }
}
