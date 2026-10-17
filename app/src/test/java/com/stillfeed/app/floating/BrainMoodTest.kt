package com.stillfeed.app.floating

import org.junit.Assert.assertEquals
import org.junit.Test

class BrainMoodTest {

    @Test
    fun `BrainMood entries have correct reference colors and properties`() {
        // Fresh
        assertEquals("#FF9DB5", BrainMood.FRESH.brainColorHex)
        assertEquals("#713A00", BrainMood.FRESH.pillColorHex)
        assertEquals("#FFB15E", BrainMood.FRESH.numColorHex)
        assertEquals("Your mind looks fresh.", BrainMood.FRESH.editorialMessage)

        // Calm
        assertEquals("#F69AB3", BrainMood.CALM.brainColorHex)
        assertEquals("#6C3A07", BrainMood.CALM.pillColorHex)
        assertEquals("#FAB26A", BrainMood.CALM.numColorHex)

        // Tired
        assertEquals("#E49AB0", BrainMood.TIRED.brainColorHex)
        assertEquals("#613A10", BrainMood.TIRED.pillColorHex)
        assertEquals("#EEB078", BrainMood.TIRED.numColorHex)

        // Sad
        assertEquals("#C79BB9", BrainMood.SAD.brainColorHex)
        assertEquals("#523A20", BrainMood.SAD.pillColorHex)
        assertEquals("#DBA886", BrainMood.SAD.numColorHex)

        // Worn out
        assertEquals("#A39AC0", BrainMood.WORN_OUT.brainColorHex)
        assertEquals("#423A33", BrainMood.WORN_OUT.pillColorHex)
        assertEquals("#BDA197", BrainMood.WORN_OUT.numColorHex)
        assertEquals("Your companion is worn out. That is the limit for today.", BrainMood.WORN_OUT.editorialMessage)
    }

    @Test
    fun `fromId resolves mood ids case-insensitively with safe fallback`() {
        assertEquals(BrainMood.FRESH, BrainMood.fromId("fresh"))
        assertEquals(BrainMood.FRESH, BrainMood.fromId("FRESH"))
        assertEquals(BrainMood.CALM, BrainMood.fromId("calm"))
        assertEquals(BrainMood.TIRED, BrainMood.fromId("tired"))
        assertEquals(BrainMood.SAD, BrainMood.fromId("sad"))
        assertEquals(BrainMood.WORN_OUT, BrainMood.fromId("worn"))
        assertEquals(BrainMood.FRESH, BrainMood.fromId("unknown_id"))
    }
}
