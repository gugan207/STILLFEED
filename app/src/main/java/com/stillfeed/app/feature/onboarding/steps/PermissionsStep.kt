package com.stillfeed.app.feature.onboarding.steps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedDivider
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedStatus
import com.stillfeed.app.core.ui.components.StillfeedStatusBadge
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 03 — Permissions (Figma Node 39:41).
 *
 * Explains Accessibility Service and Usage Access permissions, provides direct intent
 * launchers to Android system settings, re-checks actual grant state upon return,
 * and transparently explains what features remain unavailable if permissions are denied.
 */
@Composable
fun PermissionsStep(
    isAccessibilityGranted: Boolean,
    isUsageAccessGranted: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StillfeedTheme.spacing.screenMargin)
            .padding(top = StillfeedTheme.spacing.md, bottom = StillfeedTheme.spacing.xl),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StillfeedScreenHeader(
                sectionLabel = "PROTECT YOUR FEED",
                stepCounter = "03 / 05"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.section))

            Text(
                text = "Protect the apps where scrolling starts.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            Text(
                text = "STILLFEED needs permission to recognize short-video launches. Your DMs and Stories stay available.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Permission 1: Accessibility Service
            PermissionRowCard(
                title = "Accessibility Service",
                purpose = "Detects Reels and Shorts video surfaces to pause endless scrolling.",
                isGranted = isAccessibilityGranted,
                unavailableExplanation = "Without Accessibility Service, STILLFEED cannot detect when Reels or Shorts open to protect your focus.",
                onOpenSettings = { openAccessibilitySettings(context) }
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // Permission 2: Usage Access
            PermissionRowCard(
                title = "Usage Access",
                purpose = "Measures foreground app time and short-video sessions for your daily rhythm.",
                isGranted = isUsageAccessGranted,
                unavailableExplanation = "Without Usage Access, daily usage rhythms and monitored stats cannot be tracked.",
                onOpenSettings = { openUsageAccessSettings(context) }
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Protected Apps Overview (Matching Figma 39:41 list)
            Text(
                text = "PROTECTED APPS STATUS",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            StillfeedCard {
                // Instagram Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Instagram",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Text(
                            text = if (isAccessibilityGranted) "Reels blocked · DMs & Stories kept" else "Needs Accessibility permission",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                    StillfeedStatusBadge(
                        status = if (isAccessibilityGranted) StillfeedStatus.Protected else StillfeedStatus.NotConnected
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                StillfeedDivider()
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // YouTube Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "YouTube",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Text(
                            text = if (isAccessibilityGranted) "Shorts blocked · Normal videos kept" else "Needs Accessibility permission",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                    StillfeedStatusBadge(
                        status = if (isAccessibilityGranted) StillfeedStatus.Protected else StillfeedStatus.NotConnected
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                StillfeedDivider()
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // Facebook Row (Figma: Not connected)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Facebook",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Text(
                            text = "Not connected",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                    StillfeedStatusBadge(status = StillfeedStatus.NotConnected)
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // Privacy Guarantee
            Text(
                text = "Privacy guarantee: STILLFEED never reads messages, chats, captions, search queries, or passwords. Only structural app surfaces are inspected.",
                style = StillfeedTheme.typography.bodySmall,
                color = StillfeedTheme.colors.textMuted
            )
        }

        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

        StillfeedPrimaryButton(
            text = "CONTINUE",
            onClick = onContinue
        )
    }
}

@Composable
private fun PermissionRowCard(
    title: String,
    purpose: String,
    isGranted: Boolean,
    unavailableExplanation: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = StillfeedTheme.shapes.card,
        color = StillfeedTheme.colors.surface,
        border = BorderStroke(
            width = StillfeedTheme.spacing.dividerThickness,
            color = StillfeedTheme.colors.divider
        )
    ) {
        Column(
            modifier = Modifier.padding(StillfeedTheme.spacing.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = StillfeedTheme.typography.bodyLarge,
                        color = StillfeedTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = purpose,
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(StillfeedTheme.spacing.sm))

                if (isGranted) {
                    StillfeedStatusBadge(status = StillfeedStatus.Protected)
                } else {
                    Button(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StillfeedTheme.colors.buttonPrimaryBackground,
                            contentColor = StillfeedTheme.colors.buttonPrimaryText
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        )
                    ) {
                        Text(
                            text = "ENABLE",
                            style = StillfeedTheme.typography.label
                        )
                    }
                }
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))
                Text(
                    text = unavailableExplanation,
                    style = StillfeedTheme.typography.caption,
                    color = StillfeedTheme.colors.destructive
                )
            }
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback to application settings if specific intent fails
        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallback)
    }
}

private fun openUsageAccessSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
            val appFallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(appFallback)
        }
    }
}
