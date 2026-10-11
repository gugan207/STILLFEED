package com.stillfeed.app.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Calm Data Export Dialog (Figma Nodes 130:172 & 153:52).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExportDialog(
    isExporting: Boolean,
    exportJson: String?,
    onDismiss: () -> Unit,
    onCopyJson: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(StillfeedTheme.colors.surface)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isExporting) "Preparing Export" else "Export Ready",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isExporting) {
                    Text(
                        text = "Gathering your local usage events and monthly awareness history...",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(
                        color = com.stillfeed.app.core.ui.theme.StillfeedSageGreen
                    )
                } else if (exportJson != null) {
                    Text(
                        text = "Your sanitized data export (${exportJson.length} characters) is prepared. No passwords, tokens, or private node texts are included.",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    StillfeedPrimaryButton(
                        text = "Copy Export JSON",
                        onClick = onCopyJson
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StillfeedSecondaryButton(
                        text = "Done",
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

/**
 * Account Deletion Confirmation Dialog (Figma Node 147:34).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountDialog(
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(StillfeedTheme.colors.surface)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Delete Account & Data",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This will permanently remove your account, local usage events, monthly awareness history, and preferences from this device. This action cannot be undone.",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                StillfeedPrimaryButton(
                    text = "Permanently Delete",
                    onClick = onConfirmDelete
                )

                Spacer(modifier = Modifier.height(12.dp))

                StillfeedSecondaryButton(
                    text = "Keep Account",
                    onClick = onDismiss
                )
            }
        }
    }
}
