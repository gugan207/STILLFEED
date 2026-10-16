package com.stillfeed.app.feature.settings.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 27: Delete Account (Figma Node 147:34 Roadmap Card / FLOW 08: Deletion).
 */
@Composable
fun DeleteAccountScreen(
    isDeleting: Boolean,
    onConfirmDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmationText by remember { mutableStateOf("") }
    val isConfirmed = confirmationText.trim().equals("DELETE", ignoreCase = false)
    val focusManager = LocalFocusManager.current

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
                stepCounter = "03 / 03"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Delete your account.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.destructive
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "This action is permanent and cannot be undone.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            StillfeedCard {
                Text(
                    text = "WHAT GETS REMOVED",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.destructive
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "Permanent data erasure",
                    style = StillfeedTheme.typography.titleMedium,
                    color = StillfeedTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Text(
                    text = "• Cloud account and session credentials\n• All historical usage logs & unlock sessions\n• Monthly awareness rhythms & custom limits\n• Active protection guardrails will be disabled",
                    style = StillfeedTheme.typography.bodySmall,
                    color = StillfeedTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Type DELETE to confirm:",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            OutlinedTextField(
                value = confirmationText,
                onValueChange = { confirmationText = it },
                placeholder = { Text("DELETE", style = StillfeedTheme.typography.bodyMedium) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StillfeedTheme.colors.surface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StillfeedTheme.colors.destructive,
                    unfocusedBorderColor = StillfeedTheme.colors.divider
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            Button(
                onClick = onConfirmDelete,
                enabled = isConfirmed && !isDeleting,
                shape = StillfeedTheme.shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StillfeedTheme.colors.destructive,
                    contentColor = StillfeedTheme.colors.surface,
                    disabledContainerColor = StillfeedTheme.colors.destructive.copy(alpha = 0.4f),
                    disabledContentColor = StillfeedTheme.colors.surface.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(StillfeedTheme.spacing.buttonHeight)
            ) {
                Text(
                    text = if (isDeleting) "Deleting account..." else "Permanently delete account",
                    style = StillfeedTheme.typography.button
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            StillfeedSecondaryButton(
                text = "Keep my account",
                onClick = onNavigateBack
            )
        }
    }
}
