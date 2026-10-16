package com.stillfeed.app.feature.settings.subscreens

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
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.settings.components.SettingToggleRow

/**
 * Screen 31: Notification Settings (Figma Node 153:43 / FLOW 09: Supporting Settings States).
 */
@Composable
fun NotificationSettingsScreen(
    dailySummary: Boolean,
    weeklySummary: Boolean,
    activityReminders: Boolean,
    quietHours: Boolean,
    onToggleDailySummary: (Boolean) -> Unit,
    onToggleWeeklySummary: (Boolean) -> Unit,
    onToggleActivityReminders: (Boolean) -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
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
                sectionLabel = "SETTINGS",
                stepCounter = null
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Notifications.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "Choose what deserves your attention.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 1. Daily Summary
            SettingToggleRow(
                title = "Daily summary",
                subtitle = "End-of-day usage and reflection",
                checked = dailySummary,
                onCheckedChange = onToggleDailySummary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // 2. Weekly Summary
            SettingToggleRow(
                title = "Weekly summary",
                subtitle = "Weekly progress report",
                checked = weeklySummary,
                onCheckedChange = onToggleWeeklySummary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // 3. Activity Reminders
            SettingToggleRow(
                title = "Activity reminders",
                subtitle = "Gentle reminders to reset",
                checked = activityReminders,
                onCheckedChange = onToggleActivityReminders
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // 4. Important Account Alerts (Always on)
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
                            text = "Important account alerts",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Security and authentication notices",
                            style = StillfeedTheme.typography.caption,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                    Text(
                        text = "ALWAYS ON",
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            // 5. Quiet Hours
            SettingToggleRow(
                title = "Quiet hours",
                subtitle = "11:00 PM – 7:00 AM (no interruptions)",
                checked = quietHours,
                onCheckedChange = onToggleQuietHours
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            StillfeedSecondaryButton(
                text = "Back to Settings",
                onClick = onNavigateBack
            )
        }
    }
}
