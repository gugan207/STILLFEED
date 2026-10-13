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
import com.stillfeed.app.core.ui.theme.StillfeedTheme

/**
 * Screen 08 — Blocked Reel (Figma Frame 39:162).
 *
 * Visual source of truth:
 * - Header: Kicker "BLOCKED MOMENT", step "03 / 03", compact wordmark.
 * - Headline: "This is the moment to choose." (35sp, semi-bold).
 * - Charcoal card: "REEL BLOCKED" tag, "pause" badge, "Short video is paused."
 * - Primary CTA: "Choose a reset" (Watch reels / proceed to Screen 09).
 * - Secondary CTA: "Go back" (Exit reels / return to permitted feed).
 */
@Composable
fun Screen08BlockedReel(
    onChooseReset: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
    todayReelsCount: Int? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F0E7)) // Warm paper ground
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Header & Headline
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Meta Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BLOCKED MOMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFF6D746E)
                    )
                    Text(
                        text = "03 / 03",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6D746E)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Compact Wordmark
                StillfeedWordmark()

                Spacer(modifier = Modifier.height(28.dp))

                // Editorial Headline
                Text(
                    text = "This is the moment to choose.",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 40.sp,
                    color = Color(0xFF1F2421)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Dark Intervention Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1F2421))
                        .padding(24.dp)
                ) {
                    Column {
                        // Badges Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pink Accent Tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFD8A5A1))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "REEL BLOCKED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = Color(0xFF1F2421)
                                )
                            }

                            // Sage Pause Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF7D927C))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "pause",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1F2421)
                                )
                            }

                            // Verified count badge: only shown when backed by verified tracking data
                            if (todayReelsCount != null && todayReelsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF363D41))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$todayReelsCount today",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFC9CEC9)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Short video is paused.",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFBF8F2)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "The social part you came for is still available.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp,
                            color = Color(0xFFC9CEC9)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Unlock a small batch after a 30-second reset.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 18.sp,
                            color = Color(0xFF89918A)
                        )
                    }
                }
            }

            // Bottom Section: Two Distinct CTAs
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // WATCH REELS: "Choose a reset" -> Advances to Screen 09
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1F2421))
                        .clickable(onClick = onChooseReset),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Choose a reset",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp,
                        color = Color(0xFFFBF8F2)
                    )
                }

                // EXIT REELS: "Go back" -> Returns to permitted surface without unlocking
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFBF8F2))
                        .border(1.dp, Color(0xFFE5E7E1), RoundedCornerShape(14.dp))
                        .clickable(onClick = onGoBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Go back",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2421)
                    )
                }
            }
        }
    }
}
