package org.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PianoNotesTest {

    private val tolerance = 0.01

    // --- Known reference frequencies ---

    @Test
    fun `A4 is concert pitch 440Hz`() {
        assertEquals(440.0, PianoNotes["A4"]!!, tolerance)
    }

    @Test
    fun `C4 is middle C at approximately 261point63Hz`() {
        assertEquals(261.63, PianoNotes["C4"]!!, tolerance)
    }

    @Test
    fun `A0 is the lowest piano key at 27point5Hz`() {
        assertEquals(27.5, PianoNotes["A0"]!!, tolerance)
    }

    @Test
    fun `C8 is the highest piano key at approximately 4186Hz`() {
        assertEquals(4186.01, PianoNotes["C8"]!!, tolerance)
    }

    @Test
    fun `dash key maps to zero frequency for rests`() {
        assertEquals(0.0, PianoNotes["-"])
    }

    // --- Enharmonic equivalence ---

    @Test
    fun `sharp and flat enharmonics resolve to the same frequency`() {
        assertEquals(PianoNotes["C#4"], PianoNotes["Db4"])
        assertEquals(PianoNotes["D#4"], PianoNotes["Eb4"])
        assertEquals(PianoNotes["F#4"], PianoNotes["Gb4"])
        assertEquals(PianoNotes["G#4"], PianoNotes["Ab4"])
        assertEquals(PianoNotes["A#4"], PianoNotes["Bb4"])
    }

    @Test
    fun `natural notes do not have a distinct flat entry since sharp equals flat name`() {
        // "Cb4" is not generated because flatNames[0] == sharpNames[0] == "C"
        assertNull(PianoNotes["Cb4"])
        assertNull(PianoNotes["Fb4"])
    }

    // --- Structural / size sanity check ---

    @Test
    fun `map contains exactly the expected number of entries`() {
        // 88 keys: naturals contribute 1 entry each, sharps-flats contribute 2 each,
        // plus 1 entry for the "-" rest symbol.
        assertEquals(125, PianoNotes.frequencies.size)
    }

    @Test
    fun `map is not empty and contains no blank keys other than the dash`() {
        assertNotNull(PianoNotes.frequencies)
        val blankKeys = PianoNotes.frequencies.keys.filter { it.isBlank() }
        assertTrue(blankKeys.isEmpty())
    }

    // --- Unexpected / malformed inputs ---

    @Test
    fun `empty string returns null`() {
        assertNull(PianoNotes[""])
    }

    @Test
    fun `unknown note letter returns null`() {
        assertNull(PianoNotes["H4"])
    }

    @Test
    fun `out of range octave returns null`() {
        assertNull(PianoNotes["C9"])
        assertNull(PianoNotes["C-1"])
    }

    @Test
    fun `lookup is case sensitive`() {
        assertNull(PianoNotes["a4"])
        assertNull(PianoNotes["c#4"])
    }

    @Test
    fun `whitespace is not trimmed`() {
        assertNull(PianoNotes[" A4"])
        assertNull(PianoNotes["A4 "])
    }

    @Test
    fun `missing octave number returns null`() {
        assertNull(PianoNotes["A"])
        assertNull(PianoNotes["C#"])
    }

    @Test
    fun `double sharp or invalid accidental returns null`() {
        assertNull(PianoNotes["C##4"])
        assertNull(PianoNotes["Cx4"])
    }

    @Test
    fun `note with extra trailing characters returns null`() {
        assertNull(PianoNotes["A4x"])
    }
}