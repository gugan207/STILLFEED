package com.stillfeed.app.feature.edgestates

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.stillfeed.app.core.ui.components.StillfeedBottomNavigation
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedDivider
import com.stillfeed.app.core.ui.components.StillfeedNavDestination
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedStatus
import com.stillfeed.app.core.ui.components.StillfeedStatusBadge
import com.stillfeed.app.core.ui.components.StillfeedTextButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import com.stillfeed.app.feature.edgestates.model.PermissionDisabledUiState

/**
 * Screen 16 — Permission Disabled (Figma Node 70:221).
 *
 * Requirements:
 * - Represents Accessibility Service and Usage Access separately.
 * - Explains which capability is unavailable for each missing permission.
 * - Provides actions that launch the appropriate Android settings screens.
 * - Re-evaluates permission status automatically upon user return.
 * - Preserves access to the rest of STILLFEED via bottom navigation.
 */
@Composable
fun PermissionDisabledScreen(
    viewModel: PermissionDisabledViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-evaluate permissions when the user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    PermissionDisabledContent(
        uiState = uiState,
        onOpenAccessibilitySettings = {
            launchSystemSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)
        },
        onOpenUsageAccessSettings = {
            launchSystemSettings(context, Settings.ACTION_USAGE_ACCESS_SETTINGS)
        },
        onReviewAll = {
            if (!uiState.isAccessibilityGranted) {
                launchSystemSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)
            } else if (!uiState.isUsageAccessGranted) {
                launchSystemSettings(context, Settings.ACTION_USAGE_ACCESS_SETTINGS)
            } else {
                onNavigateToSettings()
            }
        },
        modifier = modifier,
        onNavigateToHome = onNavigateToHome,
        onNavigateToInsights = onNavigateToInsights,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
fun PermissionDisabledContent(
    uiState: PermissionDisabledUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onReviewAll: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
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
                        StillfeedNavDestination.Settings -> onNavigateToSettings()
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
            // 1. BRAND HEADER & SECTION
            StillfeedScreenHeader(
                sectionLabel = "PROTECTION PAUSED",
                stepCounter = "EDGE"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 2. HERO STATEMENT & BODY
            Text(
                text = uiState.headline,
                style = StillfeedTheme.typography.headlineMedium,
                color = StillfeedTheme.colors.textPrimary,
                lineHeight = 38.sp
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            Text(
                text = uiState.body,
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 3. PERMISSION SUMMARY CARD (Figma Node 70:221)
            StillfeedCard {
                Text(
                    text = "PROTECTED APPS",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                Text(
                    text = uiState.cardTitle,
                    style = StillfeedTheme.typography.titleSmall,
                    color = StillfeedTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = uiState.cardDetail,
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))
                StillfeedDivider()
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // Item A: Accessibility Service
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.accessibilityTitle,
                            style = StillfeedTheme.typography.bodyLarge,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.accessibilityDescription,
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }

                    StillfeedStatusBadge(
                        status = if (uiState.isAccessibilityGranted) StillfeedStatus.Protected else StillfeedStatus.NotConnected,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (!uiState.isAccessibilityGranted) {
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                    StillfeedTextButton(
                        text = "Open Accessibility Settings",
                        onClick = onOpenAccessibilitySettings
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                StillfeedDivider()
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                // Item B: Usage Access
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.usageAccessTitle,
                            style = StillfeedTheme.typography.bodyLarge,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.usageAccessDescription,
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }

                    StillfeedStatusBadge(
                        status = if (uiState.isUsageAccessGranted) StillfeedStatus.Protected else StillfeedStatus.NotConnected,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (!uiState.isUsageAccessGranted) {
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                    StillfeedTextButton(
                        text = "Open Usage Access Settings",
                        onClick = onOpenUsageAccessSettings
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // 4. ACTION BUTTON
            StillfeedPrimaryButton(
                text = "Review permission",
                onClick = onReviewAll
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // 5. SAFE NOTE
            Text(
                text = uiState.safeNote,
                style = StillfeedTheme.typography.caption,
                color = StillfeedTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun launchSystemSettings(context: Context, action: String) {
    try {
        val intent = Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback to general settings if specific action is unsupported
        try {
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            // OS level failure fallback
        }
    }
}
