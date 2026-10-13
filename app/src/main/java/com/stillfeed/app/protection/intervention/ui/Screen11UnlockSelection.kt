package com.stillfeed.app.protection.intervention.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfeed.app.core.ui.components.StillfeedWordmark
import com.stillfeed.app.domain.model.BatchUnlockSession

/**
 * Screen 11 — Unlock Selection (Figma Frame 64:65).
 *
 * Requirements:
 * - Success banner ("Nice reset. Your attention gets a little more room. READY badge").
 * - Batch options: 5, 10, 20, 30 Reels/Shorts.
 * - Idempotent, atomic unlock creation.
 * - Dynamic primary CTA label ("Unlock 20 reels").
 */
@Composable
fun Screen11UnlockSelection(
    selectedBatch: Int,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSelectBatch: (Int) -> Unit,
    onConfirmUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6EDD6)) // Warm cream paper ground
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Meta Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StillfeedWordmark()
                    Text(
                        text = "UNLOCK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF5E605E)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = "Choose your next limit.",
                    fontSize = 31.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF191E21)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Success Confirmation Card (Sage Green)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF84AA7A))
                        .padding(20.dp)
                ) {
                    Column {
                        // "READY" Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFBF9EF))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "READY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF191E21)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Nice reset.",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF191E21)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Your attention gets a little more room.",
                            fontSize = 14.sp,
                            color = Color(0xFF191E21)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Section Title
                Text(
                    text = "How many reels do you want to unlock?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF191E21)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4 Batch Choices (Grid / Row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BatchUnlockSession.SUPPORTED_BATCH_OPTIONS.forEach { option ->
                        val isSelected = option == selectedBatch
                        BatchChoiceCard(
                            amount = option,
                            isSelected = isSelected,
                            onClick = { onSelectBatch(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Microcopy
                Text(
                    text = "You can always come back and reset again.",
                    fontSize = 13.sp,
                    color = Color(0xFF5E605E)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 13.sp,
                        color = Color(0xFFC67284)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary CTA Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSubmitting) Color(0xFFB0C4AB) else Color(0xFF84AA7A)) // Sage Green
                    .clickable(enabled = !isSubmitting, onClick = onConfirmUnlock),
                contentAlignment = Alignment.Center
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color(0xFF191E21),
                        strokeWidth = 3.dp,
                        modifier = Modifier.height(24.dp)
                    )
                } else {
                    Text(
                        text = "Unlock $selectedBatch reels",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191E21)
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchChoiceCard(
    amount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF84AA7A) else Color(0xFFFBF9EF)
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, Color(0xFF191E21), RoundedCornerShape(14.dp))
    } else {
        Modifier.border(1.dp, Color(0xFFE5DECC), RoundedCornerShape(14.dp))
    }

    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .then(borderModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = amount.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF191E21)
            )
            Text(
                text = "reels",
                fontSize = 11.sp,
                color = if (isSelected) Color(0xFF191E21) else Color(0xFF7D807D)
            )
        }
    }
}
