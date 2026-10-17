package com.stillfeed.app.protection.intervention.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
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
import com.stillfeed.app.protection.intervention.model.ResetActivityType

/**
 * Screen 09 — Reset Activity (Figma Frame 64:7).
 *
 * Requirements:
 * - List of 4 activity choices: Flip your phone (30s), Walk (200 steps), Move (20 pushups), Read cards (20 cards).
 * - Interactive countdown execution when active.
 * - Back and cancellation handling without granting unlock.
 * - Upon verified completion, advances to Screen 10 Awareness Cards.
 */
@Composable
fun Screen09ResetActivity(
    todayReelsCount: Int?,
    selectedActivity: ResetActivityType,
    isTimerRunning: Boolean,
    remainingSeconds: Int,
    isPhoneFaceDown: Boolean = false,
    phoneRemovedNotice: String? = null,
    onSelectActivity: (ResetActivityType) -> Unit,
    onStartActivity: () -> Unit,
    onCancelActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6EDD6)) // Warm cream paper ground
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        if (isTimerRunning) {
            // Interactive in-progress activity countdown
            ResetActivityInProgress(
                activity = selectedActivity,
                remainingSeconds = remainingSeconds,
                isPhoneFaceDown = isPhoneFaceDown,
                phoneRemovedNotice = phoneRemovedNotice,
                onCancel = onCancelActivity
            )
        } else {
            // Activity selection mode
            ResetActivitySelection(
                todayReelsCount = todayReelsCount,
                selectedActivity = selectedActivity,
                onSelectActivity = onSelectActivity,
                onStartActivity = onStartActivity
            )
        }
    }
}

@Composable
private fun ResetActivitySelection(
    todayReelsCount: Int?,
    selectedActivity: ResetActivityType,
    onSelectActivity: (ResetActivityType) -> Unit,
    onStartActivity: () -> Unit
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
                    text = "RESET",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF5E605E)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = "Take a short reset.",
                fontSize = 31.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF191E21)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Usage Summary Card (Charcoal)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF191E21))
                    .padding(20.dp)
            ) {
                Column {
                    if (todayReelsCount != null && todayReelsCount > 0) {
                        Text(
                            text = todayReelsCount.toString(),
                            fontSize = 46.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFBF9EF)
                        )
                        Text(
                            text = "reels watched today",
                            fontSize = 14.sp,
                            color = Color(0xFFC6C9BC)
                        )
                    } else {
                        Text(
                            text = "Reset moment",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFBF9EF)
                        )
                        Text(
                            text = "Pause before opening short video",
                            fontSize = 14.sp,
                            color = Color(0xFFC6C9BC)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your next unlock starts with a reset.",
                        fontSize = 13.sp,
                        color = Color(0xFFADAFA5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Activity Choices (List of 4 Tiles)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResetActivityType.entries.forEach { activity ->
                    ActivityTile(
                        activity = activity,
                        isSelected = activity == selectedActivity,
                        onClick = { onSelectActivity(activity) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Helper text
            Text(
                text = "Pick the reset that feels easiest right now.",
                fontSize = 13.sp,
                color = Color(0xFF5E605E)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary CTA Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF84AA7A)) // Sage Green
                .clickable(onClick = onStartActivity),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Choose this reset",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF191E21)
            )
        }
    }
}

@Composable
private fun ActivityTile(
    activity: ResetActivityType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, Color(0xFF191E21), RoundedCornerShape(14.dp))
    } else {
        Modifier.border(1.dp, Color(0xFFE5DECC), RoundedCornerShape(14.dp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFBF9EF))
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Number Index
                Text(
                    text = activity.indexLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF5E605E)
                )

                // Accent dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(activity.accentColorValue))
                )

                // Title & Subtitle
                Column {
                    Text(
                        text = activity.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191E21)
                    )
                    Text(
                        text = activity.subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF7D807D)
                    )
                }
            }

            // Arrow Icon
            Text(
                text = "↗",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color(0xFF191E21) else Color(0xFF98A3AB)
            )
        }
    }
}

@Composable
private fun ResetActivityInProgress(
    activity: ResetActivityType,
    remainingSeconds: Int,
    isPhoneFaceDown: Boolean,
    phoneRemovedNotice: String?,
    onCancel: () -> Unit
) {
    val totalSeconds = activity.durationSeconds.coerceAtLeast(1)
    val progress = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "timerProgress"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StillfeedWordmark()
            Text(
                text = "RESETTING",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Color(0xFF5E605E)
            )
        }

        // Center Countdown & Guidance
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = activity.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF191E21)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Guidance & Sensor Status
            if (phoneRemovedNotice != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFDE8E8))
                        .border(1.dp, Color(0xFFF09595), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = phoneRemovedNotice,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFC53030)
                    )
                }
            } else if (isPhoneFaceDown) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "✓ Phone is face down · Resting your eyes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E7D32)
                    )
                }
            } else {
                Text(
                    text = when (activity) {
                        ResetActivityType.FLIP_PHONE -> "Place your phone face down on a flat surface to begin."
                        ResetActivityType.SHORT_WALK -> "Place phone face down, stand up and take 200 steps."
                        ResetActivityType.MOVE_BODY -> "Place phone face down and move your body."
                        ResetActivityType.AWARENESS_CARDS -> "Read through reflective cards."
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF5E605E),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Circular Countdown Display
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(activity.accentColorValue),
                    trackColor = Color(0xFFE5DECC),
                    strokeWidth = 6.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${remainingSeconds}s",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191E21)
                    )
                    Text(
                        text = if (!isPhoneFaceDown) "waiting" else "remaining",
                        fontSize = 12.sp,
                        color = Color(0xFF7D807D)
                    )
                }
            }
        }

        // Bottom Cancel Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFBF9EF))
                .border(1.dp, Color(0xFFE5DECC), RoundedCornerShape(14.dp))
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cancel reset",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF191E21)
            )
        }
    }
}
