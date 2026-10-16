package com.stillfeed.app.feature.settings.subscreens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 30: Change Password (Figma Node 153:40 / FLOW 09: Supporting Settings States).
 */
@Composable
fun ChangePasswordScreen(
    onUpdatePassword: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val hasLength = newPassword.length >= 8
    val hasUpper = newPassword.any { it.isUpperCase() }
    val hasSpecial = newPassword.any { !it.isLetterOrDigit() || it.isDigit() }
    val passwordsMatch = newPassword.isNotEmpty() && newPassword == confirmPassword

    val strength = when {
        hasLength && hasUpper && hasSpecial -> "Strong"
        hasLength && (hasUpper || hasSpecial) -> "Fair"
        newPassword.isNotEmpty() -> "Weak"
        else -> ""
    }

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
                stepCounter = "02 / 02"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            Text(
                text = "Password.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

            Text(
                text = "Use a strong password you can remember.",
                style = StillfeedTheme.typography.bodyMedium,
                color = StillfeedTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // New password
            Text(
                text = "New Password",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = { Text("At least 8 characters", style = StillfeedTheme.typography.bodyMedium) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(
                            text = if (isPasswordVisible) "Hide" else "Show",
                            style = StillfeedTheme.typography.caption,
                            color = StillfeedTheme.colors.textSecondary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StillfeedTheme.colors.surface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StillfeedTheme.colors.accent,
                    unfocusedBorderColor = StillfeedTheme.colors.divider
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            if (strength.isNotEmpty()) {
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Strength: ",
                        style = StillfeedTheme.typography.caption,
                        color = StillfeedTheme.colors.textMuted
                    )
                    Text(
                        text = strength,
                        style = StillfeedTheme.typography.label,
                        color = if (strength == "Strong") com.stillfeed.app.core.ui.theme.StillfeedSageGreen else StillfeedTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

            // Confirm password
            Text(
                text = "Confirm Password",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("Re-enter new password", style = StillfeedTheme.typography.bodyMedium) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StillfeedTheme.colors.surface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StillfeedTheme.colors.accent,
                    unfocusedBorderColor = StillfeedTheme.colors.divider
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            StillfeedPrimaryButton(
                text = "Update password",
                onClick = {
                    if (passwordsMatch && hasLength) {
                        onUpdatePassword(newPassword)
                        Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    } else {
                        Toast.makeText(context, "Passwords do not match or are too short", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = passwordsMatch && hasLength
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            StillfeedSecondaryButton(
                text = "Cancel",
                onClick = onNavigateBack
            )
        }
    }
}
