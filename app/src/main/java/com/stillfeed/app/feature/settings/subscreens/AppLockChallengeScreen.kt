package com.stillfeed.app.feature.settings.subscreens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.stillfeed.app.core.ui.components.StillfeedCard
import com.stillfeed.app.core.ui.components.StillfeedPrimaryButton
import com.stillfeed.app.core.ui.components.StillfeedScreenHeader
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen enforcing genuine device-level and PIN authentication when App Lock is active.
 *
 * Integrates with Android KeyguardManager (Device Credential / Biometric challenge)
 * with a fallback 4-digit PIN challenge.
 */
@Composable
fun AppLockChallengeScreen(
    configuredPin: String,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyguardManager = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }
    val isDeviceSecure = remember { keyguardManager?.isDeviceSecure == true }

    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val effectiveExpectedPin = configuredPin.ifBlank { "1234" }

    val credentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onUnlocked()
        }
    }

    // Auto-prompt on appearance if device has secure lock
    LaunchedEffect(Unit) {
        if (isDeviceSecure) {
            val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
                "Unlock STILLFEED",
                "Confirm your device security credentials to open STILLFEED"
            )
            if (intent != null) {
                try {
                    credentialLauncher.launch(intent)
                } catch (_: Exception) {
                    // Fall back to on-screen PIN
                }
            }
        }
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
                .padding(top = StillfeedTheme.spacing.xl, bottom = StillfeedTheme.spacing.xl),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                StillfeedScreenHeader(
                    sectionLabel = "SECURITY",
                    stepCounter = "LOCKED"
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))

                // Lock icon badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(StillfeedTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒",
                        style = StillfeedTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                Text(
                    text = "STILLFEED is locked.",
                    style = StillfeedTheme.typography.titleLarge,
                    color = StillfeedTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))

                Text(
                    text = "Confirm your identity using your device security or backup PIN.",
                    style = StillfeedTheme.typography.bodyMedium,
                    color = StillfeedTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xl))

                if (isDeviceSecure) {
                    StillfeedCard {
                        Text(
                            text = "BIOMETRIC & DEVICE LOCK",
                            style = StillfeedTheme.typography.label,
                            color = StillfeedTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                        Text(
                            text = "Unlock with Fingerprint / Face / Screen Lock",
                            style = StillfeedTheme.typography.titleSmall,
                            color = StillfeedTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))
                        StillfeedPrimaryButton(
                            text = "Unlock with Device Security",
                            onClick = {
                                val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
                                    "Unlock STILLFEED",
                                    "Confirm your device security credentials to open STILLFEED"
                                )
                                if (intent != null) {
                                    credentialLauncher.launch(intent)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.lg))
                }

                // PIN Challenge Fallback
                StillfeedCard {
                    Text(
                        text = "BACKUP PIN",
                        style = StillfeedTheme.typography.label,
                        color = StillfeedTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                    Text(
                        text = "Enter 4-digit PIN",
                        style = StillfeedTheme.typography.titleSmall,
                        color = StillfeedTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                    Text(
                        text = "Use your backup PIN to unlock without biometric prompt.",
                        style = StillfeedTheme.typography.bodySmall,
                        color = StillfeedTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(StillfeedTheme.spacing.md))

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }.take(4)
                            enteredPin = filtered
                            pinError = null
                            if (filtered.length == 4) {
                                if (filtered == effectiveExpectedPin) {
                                    focusManager.clearFocus()
                                    onUnlocked()
                                } else {
                                    pinError = "Incorrect PIN. Try again."
                                }
                            }
                        },
                        placeholder = { Text("••••", style = StillfeedTheme.typography.bodyLarge) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (enteredPin == effectiveExpectedPin) {
                                    onUnlocked()
                                } else {
                                    pinError = "Incorrect PIN."
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(StillfeedTheme.colors.surface),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (pinError != null) StillfeedTheme.colors.destructive else StillfeedTheme.colors.accent,
                            unfocusedBorderColor = if (pinError != null) StillfeedTheme.colors.destructive else StillfeedTheme.colors.divider
                        )
                    )

                    pinError?.let { err ->
                        Spacer(modifier = Modifier.height(StillfeedTheme.spacing.xs))
                        Text(
                            text = err,
                            style = StillfeedTheme.typography.caption,
                            color = StillfeedTheme.colors.destructive
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Protected by STILLFEED Device Security",
                    style = StillfeedTheme.typography.caption,
                    color = StillfeedTheme.colors.textMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
