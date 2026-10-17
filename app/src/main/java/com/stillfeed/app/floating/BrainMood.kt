package com.stillfeed.app.floating

/**
 * The five reference mood stages of the STILLFEED Floating Brain Companion.
 *
 * Direct match to the reference WebGL implementation:
 * - Fresh (0%): Fresh pink brain, warm caramel pill, vibrant orange count, wide smile, sparkles.
 * - Calm (25%): Soft pink, gentle smile, calm expression.
 * - Tired (50%): Desaturated pink, drooping eyelids, neutral mouth, slight head tilt.
 * - Sad (75%): Mauve-pink, furrowed brow, downward mouth, tear droplet.
 * - Worn out (100%): Muted lavender, heavy eyelids, sad mouth, double tears, floating sleeping Zs.
 */
enum class BrainMood(
    val id: String,
    val displayName: String,
    val progressThreshold: Float,
    val brainColorHex: String,
    val pillColorHex: String,
    val numColorHex: String,
    val editorialMessage: String
) {
    FRESH(
        id = "fresh",
        displayName = "Fresh",
        progressThreshold = 0.0f,
        brainColorHex = "#FF9DB5",
        pillColorHex = "#713A00",
        numColorHex = "#FFB15E",
        editorialMessage = "Your mind looks fresh."
    ),
    CALM(
        id = "calm",
        displayName = "Calm",
        progressThreshold = 0.25f,
        brainColorHex = "#F69AB3",
        pillColorHex = "#6C3A07",
        numColorHex = "#FAB26A",
        editorialMessage = "Your companion looks calm."
    ),
    TIRED(
        id = "tired",
        displayName = "Tired",
        progressThreshold = 0.50f,
        brainColorHex = "#E49AB0",
        pillColorHex = "#613A10",
        numColorHex = "#EEB078",
        editorialMessage = "Your companion looks tired."
    ),
    SAD(
        id = "sad",
        displayName = "Sad",
        progressThreshold = 0.75f,
        brainColorHex = "#C79BB9",
        pillColorHex = "#523A20",
        numColorHex = "#DBA886",
        editorialMessage = "Your companion looks sad."
    ),
    WORN_OUT(
        id = "worn",
        displayName = "Worn out",
        progressThreshold = 1.00f,
        brainColorHex = "#A39AC0",
        pillColorHex = "#423A33",
        numColorHex = "#BDA197",
        editorialMessage = "Your companion is worn out. That is the limit for today."
    );

    companion object {
        fun fromId(id: String): BrainMood {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: FRESH
        }
    }
}
