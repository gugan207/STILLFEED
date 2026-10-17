package com.stillfeed.app.protection.intervention.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfeed.app.core.ui.components.StillfeedWordmark
import com.stillfeed.app.protection.intervention.model.AwarenessCard

/**
 * Screen 10 — Awareness Cards (Figma Frame 64:46).
 *
 * Full-screen immersive editorial reflection cards:
 * 1. Full-screen card viewport filling the display.
 * 2. Enforced 2-second hold/pause on each card before scrolling/advancing to the next card.
 * 3. Calm, authoritative typography and progress tracking.
 */
@Composable
fun Screen10AwarenessCards(
    card: AwarenessCard,
    cardIndex: Int,
    totalCards: Int,
    isLastCard: Boolean,
    cardHoldRemainingSeconds: Int = 0,
    isCardHoldActive: Boolean = false,
    onNextCard: () -> Unit,
    onFinishReflection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedIndex = String.format("%02d", cardIndex + 1)
    val totalFormatted = String.format("%02d", totalCards)

    // Animated transition for full-screen card content
    AnimatedContent(
        targetState = card,
        transitionSpec = {
            (slideInHorizontally { width -> width / 2 } + fadeIn())
                .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
        },
        label = "fullScreenCardTransition",
        modifier = modifier.fillMaxSize()
    ) { activeCard ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF191E21)) // Full-screen Dark Charcoal Card
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: Wordmark & Card Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StillfeedWordmark(color = Color(0xFFFBF9EF))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2B302D))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$formattedIndex / $totalFormatted",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = Color(0xFFF2BF47)
                        )
                    }
                }

                // Center Editorial Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    // Tag Badge
                    Text(
                        text = activeCard.tag,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFFB7BAAD)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Message (Large Editorial Headline)
                    Text(
                        text = activeCard.message,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 40.sp,
                        color = Color(0xFFFBF9EF)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Supporting Reflective Copy
                    Text(
                        text = activeCard.support,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 26.sp,
                        color = Color(0xFFC6C9BF)
                    )
                }

                // Bottom Section: 2-Second Hold Indicator & Advance CTA
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 2-Second Hold / Pause Status
                    if (isCardHoldActive && cardHoldRemainingSeconds > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFFF2BF47),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Pause & reflect (${cardHoldRemainingSeconds}s)...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFC6C9BF)
                            )
                        }
                    } else {
                        Text(
                            text = "Card unlocked · Ready to advance",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF84AA7A),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Bottom Action Button
                    val isButtonEnabled = !isCardHoldActive
                    val buttonBackground = if (isButtonEnabled) Color(0xFFF2BF47) else Color(0xFF2B302D)
                    val buttonTextColor = if (isButtonEnabled) Color(0xFF191E21) else Color(0xFF7D807D)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(buttonBackground)
                            .then(
                                if (!isButtonEnabled) Modifier.border(1.dp, Color(0xFF3A403C), RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .clickable(enabled = isButtonEnabled) {
                                if (isLastCard) onFinishReflection() else onNextCard()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isCardHoldActive -> "Pause to reflect (${cardHoldRemainingSeconds}s)"
                                isLastCard -> "Continue to unlock →"
                                else -> "Next card →"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = buttonTextColor
                        )
                    }
                }
            }
        }
    }
}
