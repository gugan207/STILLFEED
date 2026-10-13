package com.stillfeed.app.protection.intervention.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.stillfeed.app.protection.intervention.model.AwarenessCard

/**
 * Screen 10 — Awareness Cards (Figma Frame 64:46).
 *
 * Requirements:
 * - Editorial card styling (Dark charcoal card on warm paper ground).
 * - Real authored reflection content.
 * - Smooth transition between cards.
 * - Progress indicator (e.g. 01 / 20 cards).
 * - Advance to Screen 11 Unlock Selection upon completion.
 */
@Composable
fun Screen10AwarenessCards(
    card: AwarenessCard,
    cardIndex: Int,
    totalCards: Int,
    isLastCard: Boolean,
    onNextCard: () -> Unit,
    onFinishReflection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedIndex = String.format("%02d", cardIndex + 1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6EDD6)) // Warm cream paper ground
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Header & Progress
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Meta Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StillfeedWordmark()
                    Text(
                        text = "AWARENESS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF5E605E)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = "Slow the scroll.",
                    fontSize = 31.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF191E21)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedIndex,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191E21)
                    )
                    Text(
                        text = "/ $totalCards cards",
                        fontSize = 13.sp,
                        color = Color(0xFF5E605E)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Animated Awareness Card
                AnimatedContent(
                    targetState = card,
                    transitionSpec = {
                        (slideInHorizontally { width -> width / 2 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
                    },
                    label = "cardTransition"
                ) { activeCard ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF191E21))
                            .padding(24.dp)
                    ) {
                        Column {
                            // Tag
                            Text(
                                text = activeCard.tag,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                color = Color(0xFFB7BAAD)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Message
                            Text(
                                text = activeCard.message,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 34.sp,
                                color = Color(0xFFFBF9EF)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Supporting copy
                            Text(
                                text = activeCard.support,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 22.sp,
                                color = Color(0xFFC6C9BF)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Reflection Indicator Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2B302D))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Pause & reflect",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFBF9EF)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Footer Note
                Text(
                    text = "Awareness is an unlock activity — not a test.",
                    fontSize = 13.sp,
                    color = Color(0xFF5E605E)
                )
            }

            // Bottom CTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF2BF47)) // Warm Yellow
                    .clickable(onClick = {
                        if (isLastCard) onFinishReflection() else onNextCard()
                    }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLastCard) "Continue to unlock" else "Next card",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF191E21)
                )
            }
        }
    }
}
