package com.stillfeed.app.feature.settings.subscreens

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.settings.components.SettingToggleRow

/**
 * Screen 25: App Lock (Figma Node 130:190 / FLOW 07: Security / Support).
 */
@Composable
fun AppLockScreen(
    appLockEnabled: Boolean,
    biometricEnabled: Boolean,
    pinConfigured: Boolean,
    autoLockTimeoutMinutes: Int,
    onToggleAppLock: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onSelectAutoLockTimeout: (Int) -> Unit,
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
                sectionLabel = "SECURITY",
                stepCounter = "01 / 02"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Keep it private.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "Use the device security you already trust.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 1. App Lock Toggle
            SettingToggleRow(
                title = "App Lock",
                subtitle = "Require authentication to open STILLFEED",
                checked = appLockEnabled,
                onCheckedChange = onToggleAppLock
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // 2. Biometric Unlock
            SettingToggleRow(
                title = "Biometric unlock",
                subtitle = "Fingerprint or Face Recognition",
                checked = biometricEnabled,
                onCheckedChange = onToggleBiometric,
                enabled = appLockEnabled
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // 3. 4-Digit PIN
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(StillfeedTheme.colors.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "4-digit backup PIN",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (pinConfigured) "Configured on device" else "Default PIN (1234)",
                            style = StillfeedTheme.typography.caption,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                    Text(
                        text = if (pinConfigured) "Set" else "Active",
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // 4. Auto-lock timeout cycle
            val timeoutLabel = when (autoLockTimeoutMinutes) {
                0 -> "Immediately"
                1 -> "1 minute"
                5 -> "5 minutes"
                else -> "$autoLockTimeoutMinutes minutes"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(StillfeedTheme.colors.surface)
                    .clickable(enabled = appLockEnabled) {
                        val next = when (autoLockTimeoutMinutes) {
                            0 -> 1
                            1 -> 5
                            else -> 0
                        }
                        onSelectAutoLockTimeout(next)
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
                            text = "Auto-lock delay",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Lock when switching apps",
                            style = StillfeedTheme.typography.caption,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                    Text(
                        text = timeoutLabel,
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // Explainer Card
            StillfeedCard {
                Text(
                    text = "SMART BEHAVIOR",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Calm and uninterrupted",
                    style = StillfeedTheme.typography.titleSmall,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Stay unlocked while actively using STILLFEED. Phone lock or device restart locks it automatically.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            StillfeedSecondaryButton(
                text = "Back to Settings",
                onClick = onNavigateBack
            )
        }
    }
}
