package com.stillfeed.app.protection.intervention.model

/**
 * Model representing a reflective Awareness Card (Figma Screen 10 / Node 64:46).
 *
 * Requirements:
 * - Editorial card styling.
 * - Thoughtful, calm reflection prompts.
 * - Zero shame, guilt, failure, or clinical diagnostic language.
 */
data class AwarenessCard(
    val id: Int,
    val tag: String,
    val message: String,
    val support: String
)

object AwarenessCardCatalog {
    val cards: List<AwarenessCard> = listOf(
        AwarenessCard(
            id = 1,
            tag = "YOUR ATTENTION",
            message = "Short-form feeds are designed to keep you moving.",
            support = "A few seconds of awareness gives your brain a chance to choose what comes next."
        ),
        AwarenessCard(
            id = 2,
            tag = "INTENTIONAL CHOICE",
            message = "Notice how your body feels right now.",
            support = "Are your shoulders tense? Is your breathing shallow? Let yourself take one full breath."
        ),
        AwarenessCard(
            id = 3,
            tag = "CALM SPACES",
            message = "The feed will always have another video waiting.",
            support = "Your attention does not replenish automatically. It is the only finite asset you bring here."
        ),
        AwarenessCard(
            id = 4,
            tag = "PURPOSE",
            message = "What did you pick up your phone to do?",
            support = "If you came to send a message or check on someone, that intention is still waiting for you."
        ),
        AwarenessCard(
            id = 5,
            tag = "FRICTION AS CARE",
            message = "This pause is not a punishment.",
            support = "It is a deliberate boundary designed to return agency to your hands."
        ),
        AwarenessCard(
            id = 6,
            tag = "PACING",
            message = "You don't have to quit entirely.",
            support = "Unlocking a small batch gives you enjoyment with clear edges, instead of an endless slide."
        ),
        AwarenessCard(
            id = 7,
            tag = "RHYTHM",
            message = "Attention is like an inhale and exhale.",
            support = "Continuous consumption is all inhale. Take a moment to let the mind exhale."
        ),
        AwarenessCard(
            id = 8,
            tag = "OBSERVATION",
            message = "Look up from the glass for five seconds.",
            support = "Notice the light in your room, the surface under your hands, or sounds in the distance."
        ),
        AwarenessCard(
            id = 9,
            tag = "ALGORITHM",
            message = "Recommended feeds respond to reaction, not intention.",
            support = "Every pause is a gentle reminder that you decide what deserves your focus."
        ),
        AwarenessCard(
            id = 10,
            tag = "ENERGY",
            message = "Does scrolling recharge you, or does it leave you drained?",
            support = "Be honest with how you feel after twenty videos versus two."
        ),
        AwarenessCard(
            id = 11,
            tag = "THE PRESENT",
            message = "Whatever was waiting for you before you opened the app is still there.",
            support = "Take a breath and decide whether to step back into it."
        ),
        AwarenessCard(
            id = 12,
            tag = "GENTLE EDGES",
            message = "Setting a limit creates a natural stopping point.",
            support = "When this unlock finishes, you can choose again without pressure."
        ),
        AwarenessCard(
            id = 13,
            tag = "COMMUNICATION",
            message = "Your friends and chats are always available.",
            support = "Stillfeed protects short-form autoplay while keeping your human connections open."
        ),
        AwarenessCard(
            id = 14,
            tag = "STILLNESS",
            message = "Silence and stillness are not empty.",
            support = "They are the baseline where creativity, memory, and calm reset."
        ),
        AwarenessCard(
            id = 15,
            tag = "PERSPECTIVE",
            message = "Ten minutes from now, will you remember the last five videos?",
            support = "If not, give yourself permission to step away with a clear head."
        ),
        AwarenessCard(
            id = 16,
            tag = "AGENCY",
            message = "You are in control of this unlock.",
            support = "Choose a batch that fits your moment, enjoy it, and let the boundary hold."
        ),
        AwarenessCard(
            id = 17,
            tag = "CLARITY",
            message = "Notice the urge to keep swiping.",
            support = "Just observing that pull diminishes its power over your actions."
        ),
        AwarenessCard(
            id = 18,
            tag = "SELF-RESPECT",
            message = "Protecting your time is an act of care.",
            support = "You deserve a phone that supports your life rather than competing for it."
        ),
        AwarenessCard(
            id = 19,
            tag = "QUIET CONFIDENCE",
            message = "Small resets compound over weeks.",
            support = "Every time you slow the scroll, you rebuild the habit of deliberate focus."
        ),
        AwarenessCard(
            id = 20,
            tag = "CLOSING CIRCLE",
            message = "You took the time to reset.",
            support = "Your mind has space now. Step forward with whatever limit feels right."
        )
    )
}
