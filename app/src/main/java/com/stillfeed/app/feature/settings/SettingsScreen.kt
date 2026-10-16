package com.stillfeed.app.feature.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedBottomNavigation
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedHeader
import com.stillfeed.app.core.ui.components.StillfeedNavDestination
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.domain.model.MonthlyAwarenessStatus
import com.stillfeed.app.feature.settings.components.DataExportDialog
import com.stillfeed.app.feature.settings.components.DeleteAccountDialog
import com.stillfeed.app.feature.settings.components.SettingToggleRow

/**
 * STILLFEED Settings Screen (Figma Screen 13 / Node 70:149 and Screen 16 / Node 70:221).
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToAwarenessSelection: () -> Unit,
    onSignOutOrDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfileSecurity: () -> Unit = {},
    onNavigateToPrivacyCenter: () -> Unit = {},
    onNavigateToAppLock: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToDataExport: () -> Unit = {},
    onNavigateToDeleteAccount: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Re-check system permissions and load latest settings on initial composition / resume
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
        viewModel.loadSettings()
    }

    // Handle sign out or account deletion navigation
    LaunchedEffect(uiState.signedOut) {
        if (uiState.signedOut) {
            onSignOutOrDelete()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary,
        bottomBar = {
            StillfeedBottomNavigation(
                selectedDestination = StillfeedNavDestination.Settings,
                onDestinationSelected = { destination ->
                    when (destination) {
                        StillfeedNavDestination.Home -> onNavigateToHome()
                        StillfeedNavDestination.Insights -> onNavigateToInsights()
                        StillfeedNavDestination.Settings -> { /* Already on Settings */ }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = StillfeedTheme.spacing.screenMargin)
                .padding(top = StillfeedTheme.spacing.lg, bottom = StillfeedTheme.spacing.xl)
        ) {
            // Header (Node 70:151 - 70:154)
            StillfeedHeader(
                sectionLabel = "SETTINGS",
                stepCounter = "03 / 03"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Make it work for you.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "Keep the guardrails simple.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // EDGE STATE: Screen 16 (Node 70:221) "Your protection is paused"
            if (uiState.isProtectionPausedDueToPermissions) {
                StillfeedCard(
                    modifier = Modifier.padding(bottom = StillfeedTheme.spacing.lg)
                ) {
                    Text(
                        text = "PROTECTED APPS",
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                    Text(
                        text = "Access needs attention.",
                        style = StillfeedTheme.typography.titleMedium,
                        color = StillfeedTheme.colors.textPrimary
                    )
                    Text(
                        text = "SKILLFEED needs permission to keep short video blocked. Your Stories and DMs remain yours.",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                    StillfeedPrimaryButton(
                        text = "Review permission",
                        onClick = {
                            openSystemSettings(context, uiState)
                        }
                    )
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                    Text(
                        text = "Nothing is lost while protection is paused.",
                        style = StillfeedTheme.typography.caption,
                        color = StillfeedTheme.colors.textMuted
                    )
                }
            }

            // 1. SETTING ROWS (Nodes 70:155 - 70:172)
            // Row 1: Protected apps
            SettingToggleRow(
                title = "Protected apps",
                subtitle = "Instagram · YouTube",
                checked = uiState.protectionEnabled,
                onCheckedChange = { viewModel.setProtectionEnabled(it) }
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // Row 2: Stories & DMs
            SettingToggleRow(
                title = "Stories & DMs",
                subtitle = "Stay available",
                checked = uiState.storiesAndDmsAvailable,
                onCheckedChange = null, // Fixed per product rule
                enabled = false
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // Row 3: Monthly awareness limit
            val awareness = uiState.monthlyAwarenessState
            val awarenessSubtitle = when {
                awareness == null || awareness.status == MonthlyAwarenessStatus.BASELINE_FORMING ->
                    "Establishing baseline rhythm · Reviewed monthly"
                awareness.targetReductionPercentage > 0 ->
                    "${awareness.targetReductionPercentage}% reduction · Fixed for this month"
                else ->
                    "Fixed for this month; reviewed monthly"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(StillfeedTheme.colors.surface)
                    .clickable { onNavigateToAwarenessSelection() }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly awareness limit",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = awarenessSubtitle,
                            style = StillfeedTheme.typography.caption,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                    Text(
                        text = if (awareness?.targetReductionPercentage ?: 0 > 0) "-${awareness?.targetReductionPercentage}%" else "Edit",
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // Row 4: Appearance / Night mode (Node 153:46)
            val currentTheme = uiState.preferredThemeMode
            val themeDetail = when (currentTheme) {
                "LIGHT" -> "Light · Warm paper"
                "DARK" -> "Dark · Low-light"
                else -> "Follows your system setting"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(StillfeedTheme.colors.surface)
                    .clickable {
                        // Cycle SYSTEM -> LIGHT -> DARK -> SYSTEM
                        val next = when (currentTheme) {
                            "SYSTEM" -> "LIGHT"
                            "LIGHT" -> "DARK"
                            else -> "SYSTEM"
                        }
                        viewModel.setThemeMode(next)
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Night mode",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = themeDetail,
                            style = StillfeedTheme.typography.caption,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                    Text(
                        text = currentTheme,
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // MORE SETTINGS (Screens 22, 23, 25, 26, 31, 32)
            Text(
                text = "MORE SETTINGS",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            SettingsNavRow(
                title = "Profile & Security",
                subtitle = "Name, password, and active devices",
                onClick = onNavigateToProfileSecurity
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            SettingsNavRow(
                title = "Privacy Center",
                subtitle = "Data storage policies and portable exports",
                onClick = onNavigateToPrivacyCenter
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            SettingsNavRow(
                title = "App Lock",
                subtitle = "Biometrics, backup PIN, and auto-lock delay",
                onClick = onNavigateToAppLock
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            SettingsNavRow(
                title = "Notifications",
                subtitle = "Summaries, reminders, and quiet hours",
                onClick = onNavigateToNotifications
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            SettingsNavRow(
                title = "Appearance",
                subtitle = "Light, dark, and system theme selection",
                onClick = onNavigateToAppearance
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            SettingsNavRow(
                title = "Help & Support",
                subtitle = "WhatsApp chat, email support, and FAQ",
                onClick = onNavigateToSupport
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // 2. PERMISSIONS REVIEW (Node 70:173 - 70:175)
            Text(
                text = "Need to change access?",
                style = StillfeedTheme.typography.bodySmall,
                color = StillfeedTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
            StillfeedPrimaryButton(
                text = "Review permissions",
                onClick = {
                    openSystemSettings(context, uiState)
                }
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // 3. ACCOUNT & DATA MANAGEMENT (Nodes 130:161 - 130:164)
            Text(
                text = "ACCOUNT & PRIVACY",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            StillfeedCard {
                val email = uiState.currentUser?.email ?: "Signed In"
                Text(
                    text = email,
                    style = StillfeedTheme.typography.titleSmall,
                    color = StillfeedTheme.colors.textPrimary
                )
                Text(
                    text = "Personal details stay under your control.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // Download my data (Node 130:162)
                StillfeedSecondaryButton(
                    text = "Download my data",
                    onClick = {
                        viewModel.exportData()
                        showExportDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                // Sign out
                StillfeedSecondaryButton(
                    text = "Sign out",
                    onClick = { viewModel.signOut() }
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                // Delete account & data (Node 130:164)
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Delete my account",
                        style = StillfeedTheme.typography.bodyMedium,
                        color = StillfeedTheme.colors.destructive
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // Footer (Node 70:176)
            Text(
                text = "SKILLFEED · Calm by design",
                style = StillfeedTheme.typography.caption,
                color = StillfeedTheme.colors.textMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Dialogs
        if (showExportDialog) {
            DataExportDialog(
                isExporting = uiState.isExporting,
                exportJson = uiState.exportJsonResult,
                onDismiss = {
                    showExportDialog = false
                    viewModel.clearExportResult()
                },
                onCopyJson = {
                    uiState.exportJsonResult?.let { json ->
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(context, "Export copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (showDeleteDialog) {
            DeleteAccountDialog(
                onConfirmDelete = {
                    showDeleteDialog = false
                    viewModel.deleteAccountAndData()
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

private fun openSystemSettings(context: Context, uiState: SettingsUiState) {
    try {
        if (!uiState.isAccessibilityPermissionGranted) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    } catch (_: Exception) {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StillfeedTheme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = StillfeedTheme.typography.titleSmall,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = StillfeedTheme.typography.caption,
                    color = StillfeedTheme.colors.textSecondary
                )
            }
            Text(
                text = "›",
                style = StillfeedTheme.typography.titleMedium,
                color = StillfeedTheme.colors.textSecondary
            )
        }
    }
}
