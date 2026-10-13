package com.stillfeed.app.protection.intervention.model

/**
 * Activity options available on Figma Screen 09 — Reset Activity (Node 64:7).
 */
enum class ResetActivityType(
    val indexLabel: String,
    val title: String,
    val subtitle: String,
    val durationSeconds: Int,
    val accentColorValue: Long
) {
    FLIP_PHONE(
        indexLabel = "01",
        title = "Flip your phone",
        subtitle = "30 seconds",
        durationSeconds = 30,
        accentColorValue = 0xFFF2BF47 // Warm Yellow
    ),
    SHORT_WALK(
        indexLabel = "02",
        title = "Take a short walk",
        subtitle = "200 steps",
        durationSeconds = 45,
        accentColorValue = 0xFF84AA7A // Sage Green
    ),
    MOVE_BODY(
        indexLabel = "03",
        title = "Move your body",
        subtitle = "20 pushups",
        durationSeconds = 40,
        accentColorValue = 0xFFC67284 // Soft Pink
    ),
    AWARENESS_CARDS(
        indexLabel = "04",
        title = "Read awareness cards",
        subtitle = "20 cards",
        durationSeconds = 0, // Governed by card reading
        accentColorValue = 0xFF9B8CCC // Lavender
    )
}
