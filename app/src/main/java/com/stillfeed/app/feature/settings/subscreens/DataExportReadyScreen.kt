package com.stillfeed.app.feature.settings.subscreens

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.components.StillfeedTagBadge
import com.stillfeed.app.core.ui.components.StillfeedTagType
import com.stillfeed.app.core.ui.theme.StillfeedTheme
import java.io.File
import java.io.FileOutputStream

/**
 * Screen 34: Data Export Ready (Figma Node 153:52 / FLOW 09: Supporting Settings States).
 */
@Composable
fun DataExportReadyScreen(
    exportJson: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
                stepCounter = "04 / 04"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Export ready.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "Download the files you requested.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StillfeedTagBadge(text = "EXPORT READY", type = StillfeedTagType.Kept)
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = "Generated just now",
                    style = StillfeedTheme.typography.caption,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // Card 1: Share / Save JSON export file
            StillfeedCard {
                Text(
                    text = "COMPLETE DATA EXPORT",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Portable JSON Archive",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Contains profile, room usage logs, awareness monthly goals, and app preferences.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                StillfeedPrimaryButton(
                    text = "Share or Save File",
                    onClick = {
                        exportJson?.let { json ->
                            shareExportFile(context, json)
                        } ?: Toast.makeText(context, "Export data not ready yet", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // Card 2: Copy to Clipboard
            StillfeedCard {
                Text(
                    text = "QUICK COPY",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Copy text payload",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Copy formatted JSON directly to clipboard to inspect or save manually.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                StillfeedSecondaryButton(
                    text = "Copy JSON",
                    onClick = {
                        exportJson?.let { json ->
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "Export copied to clipboard", Toast.LENGTH_SHORT).show()
                        } ?: Toast.makeText(context, "Export data not ready yet", Toast.LENGTH_SHORT).show()
                    }
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

private fun shareExportFile(context: Context, jsonContent: String) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "STILLFEED Data Export")
            putExtra(Intent.EXTRA_TEXT, jsonContent)
        }
        val chooser = Intent.createChooser(sendIntent, "Share STILLFEED Data Export")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing export: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
