package com.stillfeed.app.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.stillfeed.app.auth.model.AuthState
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedDivider
import com.stillfeed.app.core.ui.components.StillfeedHeader
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedSecondaryButton
import com.stillfeed.app.core.ui.components.StillfeedStatus
import com.stillfeed.app.core.ui.components.StillfeedStatusBadge
import com.stillfeed.app.core.ui.components.StillfeedTagBadge
import com.stillfeed.app.core.ui.components.StillfeedTagType
import com.stillfeed.app.core.ui.components.StillfeedTextButton
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Temporary development authentication screen.
 *
 * Implements the minimum required UI to test the authentication state flow
 * while adhering strictly to the established STILLFEED design system primitives.
 * (Figma is locked and does not have dedicated production sign-in screens).
 */
@Composable
fun DevAuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.currentAuthState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StillfeedTheme.colors.backgroundPrimary
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = StillfeedTheme.spacing.screenMargin)
                .padding(top = 16.dp, bottom = 20.dp)
        ) {
            // Header Hierarchy (STILLFEED -> Section Label -> Step Counter)
            StillfeedHeader(
                sectionLabel = if (uiState.isSignUpMode) "Create account" else "Account access",
                stepCounter = "01 / 01"
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            Text(
                text = "YOUR ATTENTION IS STILL YOURS",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
            Text(
                text = if (uiState.isSignUpMode) "Begin with a quieter feed." else "Welcome back to calm.",
                style = StillfeedTheme.typography.titleLarge,
                color = StillfeedTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

            // Auth Input Card
            StillfeedCard {
                if (uiState.isSignUpMode) {
                    Text(
                        text = "Your name",
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                    OutlinedTextField(
                        value = uiState.displayNameInput,
                        onValueChange = viewModel::onDisplayNameChanged,
                        placeholder = {
                            Text(
                                "Your name",
                                style = StillfeedTheme.typography.bodyMedium,
                                color = StillfeedTheme.colors.textMuted
                            )
                        },
                        singleLine = true,
                        textStyle = StillfeedTheme.typography.bodyMedium,
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                }

                Text(
                    text = "Email address",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                OutlinedTextField(
                    value = uiState.emailInput,
                    onValueChange = viewModel::onEmailChanged,
                    placeholder = {
                        Text(
                            "you@example.com",
                            style = StillfeedTheme.typography.bodyMedium,
                            color = StillfeedTheme.colors.textMuted
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    textStyle = StillfeedTheme.typography.bodyMedium,
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                Text(
                    text = "Password",
                    style = StillfeedTheme.typography.label,
                    color = StillfeedTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                OutlinedTextField(
                    value = uiState.passwordInput,
                    onValueChange = viewModel::onPasswordChanged,
                    placeholder = {
                        Text(
                            if (uiState.isSignUpMode) "At least 8 characters" else "Your password",
                            style = StillfeedTheme.typography.bodyMedium,
                            color = StillfeedTheme.colors.textMuted
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    textStyle = StillfeedTheme.typography.bodyMedium,
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error Presentation (Calm & respectful)
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = StillfeedTheme.colors.destructive.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = StillfeedTheme.typography.bodySmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

                // Submit Button
                StillfeedPrimaryButton(
                    text = when {
                        uiState.isLoading -> "Verifying..."
                        uiState.isSignUpMode -> "Create account"
                        else -> "Sign in"
                    },
                    onClick = viewModel::submit,
                    enabled = !uiState.isLoading && uiState.isInputValid
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

                // Mode toggle
                StillfeedTextButton(
                    text = if (uiState.isSignUpMode) {
                        "Already have an account? Sign in"
                    } else {
                        "New to STILLFEED? Create account"
                    },
                    onClick = viewModel::toggleAuthMode,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

            // Development Inspection Panel
            Text(
                text = "DEVELOPMENT STATE INSPECTION",
                style = StillfeedTheme.typography.label,
                color = StillfeedTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))

            StillfeedCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current State:",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )
                    when (authState) {
                        is AuthState.SignedIn -> StillfeedTagBadge(text = "SIGNED IN", type = StillfeedTagType.Kept)
                        is AuthState.SignedOut -> StillfeedTagBadge(text = "SIGNED OUT", type = StillfeedTagType.Blocked)
                        is AuthState.Loading -> Text("Loading...", style = StillfeedTheme.typography.caption)
                        is AuthState.SigningIn -> Text("Signing In...", style = StillfeedTheme.typography.caption)
                        is AuthState.SigningUp -> Text("Signing Up...", style = StillfeedTheme.typography.caption)
                        is AuthState.SessionExpired -> Text("Expired", style = StillfeedTheme.typography.caption)
                        is AuthState.Error -> Text("Error", style = StillfeedTheme.typography.caption)
                    }
                }

                if (authState is AuthState.SignedIn) {
                    val user = (authState as AuthState.SignedIn).user
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))
                    Text(
                        text = "User: ${user.email} (ID: ${user.id})",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                    Text(
                        text = "Device Bound: ${if (user.isDeviceBound) user.deviceBindingId else "None (unbound)"}",
                        style = StillfeedTheme.typography.caption,
                        color = StillfeedTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(StillfeedTheme.spacing.sm)
                    ) {
                        StillfeedSecondaryButton(
                            text = "Bind Current Device",
                            onClick = { viewModel.bindCurrentDevice("device_pixel_local_${System.currentTimeMillis() % 10000}") },
                            modifier = Modifier.weight(1f)
                        )
                        StillfeedSecondaryButton(
                            text = "Sign Out",
                            onClick = viewModel::signOut,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.sm))
                    StillfeedSecondaryButton(
                        text = "Fill Test Account",
                        onClick = {
                            viewModel.onEmailChanged("demo@stillfeed.app")
                            viewModel.onPasswordChanged("stillfeed2026")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = StillfeedTheme.colors.accent,
    unfocusedBorderColor = StillfeedTheme.colors.divider,
    focusedTextColor = StillfeedTheme.colors.textPrimary,
    unfocusedTextColor = StillfeedTheme.colors.textPrimary,
    cursorColor = StillfeedTheme.colors.textPrimary,
    focusedContainerColor = StillfeedTheme.colors.surface,
    unfocusedContainerColor = StillfeedTheme.colors.surface
)
