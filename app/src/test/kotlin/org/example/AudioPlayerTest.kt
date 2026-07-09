package org.example

import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertIs

private fun AudioPlayer.callPrivate(name: String, vararg args: Any?): Any? {
    val method = AudioPlayer::class.java.declaredMethods.first { it.name == name && it.parameterCount == args.size }
    method.isAccessible = true
    return try {
        method.invoke(this, *args)
    } catch (e: InvocationTargetException) {
        throw e.targetException // unwrap so assertFailsWith sees the real exception type
    }
}

class AudioPlayerTest {

    private val player = AudioPlayer()
    private val header = SongHeader(sampleRate = 100, beatsPerMeasure = 4, tempo = 60)

    private fun tempFile(content: String): File =
        File.createTempFile("song", ".txt").apply { writeText(content); deleteOnExit() }

    // ================= readFileLines =================

    @Test
    fun `readFileLines returns lines for an existing file`() {
        val file = tempFile("100 4 60\nsin | C4 1.0")
        val result = player.callPrivate("readFileLines", file.path) as List<*>
        assertEquals(listOf("100 4 60", "sin | C4 1.0"), result)
    }

    @Test
    fun `readFileLines returns empty list for a nonexistent file instead of throwing`() {
        val result = player.callPrivate("readFileLines", "/no/such/path/definitely.txt") as List<*>
        assertTrue(result.isEmpty())
    }

    @Test
    fun `readFileLines returns empty list for a directory path`() {
        // Passing a directory instead of a file is another malformed-input case.
        val result = player.callPrivate("readFileLines", System.getProperty("java.io.tmpdir")) as List<*>
        assertTrue(result.isEmpty())
    }

    // ================= parseHeader (via reflection) =================

    @Test
    fun `parseHeader parses a well-formed header`() {
        val result = player.callPrivate("parseHeader", "100 4 60") as SongHeader
        assertEquals(SongHeader(100, 4, 60), result)
    }

    @Test
    fun `parseHeader tolerates extra whitespace between tokens`() {
        val result = player.callPrivate("parseHeader", "  100   4  60 ") as SongHeader
        assertEquals(SongHeader(100, 4, 60), result)
    }

    @Test
    fun `parseHeader throws on wrong token count`() {
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseHeader", "100 4") }
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseHeader", "100 4 60 999") }
    }

    @Test
    fun `parseHeader throws on non-numeric tokens`() {
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseHeader", "abc 4 60") }
    }

    @Test
    fun `parseHeader throws on non-positive values`() {
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseHeader", "0 4 60") }
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseHeader", "100 -4 60") }
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseHeader", "100 4 0") }
    }

    @Test
    fun `parseHeader throws on empty line`() {
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseHeader", "") }
    }

    // ================= getWaveformStrategy =================

    @Test
    fun `getWaveformStrategy returns correct strategy for each known name`() {
        assertIs<SineStrategy>(player.callPrivate("getWaveformStrategy", "sin"))
        assertIs<SquareStrategy>(player.callPrivate("getWaveformStrategy", "square"))
        assertIs<SawtoothStrategy>(player.callPrivate("getWaveformStrategy", "saw"))
        assertIs<WhiteNoiseStrategy>(player.callPrivate("getWaveformStrategy", "whitenoise"))
    }

    @Test
    fun `getWaveformStrategy returns null for unknown or malformed names`() {
        assertNull(player.callPrivate("getWaveformStrategy", "SIN")) // case sensitive
        assertNull(player.callPrivate("getWaveformStrategy", ""))
        assertNull(player.callPrivate("getWaveformStrategy", "sine"))
        assertNull(player.callPrivate("getWaveformStrategy", " sin"))
    }

    // ================= parseMeasure =================

    @Test
    fun `parseMeasure parses well-formed note-duration pairs`() {
        val result = player.callPrivate("parseMeasure", "C4 1.0 D4 0.5") as List<*>
        assertEquals(2, result.size)
    }

    @Test
    fun `parseMeasure throws on odd token count`() {
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseMeasure", "C4 1.0 D4") }
    }

    @Test
    fun `parseMeasure throws on non-numeric duration`() {
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseMeasure", "C4 fast") }
    }

    @Test
    fun `parseMeasure throws on non-positive duration`() {
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseMeasure", "C4 0.0") }
        assertFailsWith<IllegalArgumentException> { player.callPrivate("parseMeasure", "C4 -1.0") }
    }

    @Test
    fun `parseMeasure on empty or blank string returns empty list rather than throwing`() {
        val result = player.callPrivate("parseMeasure", "   ") as List<*>
        assertTrue(result.isEmpty())
    }

    // ================= parseChannel =================

    @Test
    fun `parseChannel returns a Channel for a well-formed line with no effects`() {
        val result = player.callPrivate("parseChannel", "sin | C4 1.0", header)
        assertIs<Channel>(result)
    }

    @Test
    fun `parseChannel returns null for unknown waveform name`() {
        val result = player.callPrivate("parseChannel", "kazoo | C4 1.0", header)
        assertNull(result)
    }

    @Test
    fun `parseChannel returns null instead of throwing when a measure is malformed`() {
        // parseMeasure throws IllegalArgumentException internally; parseChannel catches
        // it and returns null (this differs from earlier standalone versions that threw).
        val result = player.callPrivate("parseChannel", "sin | C4 notanumber", header)
        assertNull(result)
    }

    @Test
    fun `parseChannel handles a channel with only settings and no note segments`() {
        val result = player.callPrivate("parseChannel", "sin", header)
        assertIs<Channel>(result)
        assertTrue((result as Channel).samples().isEmpty())
    }

    @Test
    fun `parseChannel with a single valid effect applies the decorator`() {
        val result = player.callPrivate("parseChannel", "sin vol$0.5 | C4 1.0", header)
        assertIs<VolumeDecorator>(result)
    }

    @Test
    fun `parseChannel returns null when an effect has an unparseable parameter`() {
        val result = player.callPrivate("parseChannel", "sin vol\$loud | C4 1.0", header)
        assertNull(result)
    }

    @Test
    fun `parseChannel returns null when an effect is missing required parameters`() {
        // "vol" with no "$level" segment -> params is empty -> params[0] throws
        // IndexOutOfBoundsException inside processEffects, caught, returns null.
        val result = player.callPrivate("parseChannel", "sin vol | C4 1.0", header)
        assertNull(result)
    }

    @Test
    fun `parseChannel ignores unknown effect names without failing`() {
        val result = player.callPrivate("parseChannel", "sin reverb\$0.5 | C4 1.0", header)
        assertIs<Channel>(result) // unknown effect is skipped, base Channel returned untouched
    }

    @Test
    fun `parseChannel chains multiple effects in order`() {
        val result = player.callPrivate("parseChannel", "sin vol\$0.8 tanh\$2.0 | C4 1.0", header)
        assertIs<TanhDecorator>(result) // outermost decorator reflects the last effect applied
    }

    // ================= processEffects =================

    @Test
    fun `processEffects with empty list returns the original sound unchanged`() {
        val channel = Channel(SineStrategy(), listOf(Note("C4", 1.0)), header)
        val result = player.callPrivate("processEffects", emptyList<String>(), channel, header, emptyList<Int>())
        assertEquals(channel, result) // same reference, no wrapping applied
    }

    // ================= mix =================

    @Test
    fun `mix sums samples across multiple equal-length channels`() {
        val a = doubleArrayOf(1.0, 2.0, 3.0)
        val b = doubleArrayOf(0.5, 0.5, 0.5)
        val result = player.callPrivate("mix", listOf(a, b)) as DoubleArray
        assertEquals(listOf(1.5, 2.5, 3.5), result.toList())
    }

    @Test
    fun `mix pads shorter channels with silence rather than throwing`() {
        val long = doubleArrayOf(1.0, 1.0, 1.0, 1.0)
        val short = doubleArrayOf(0.5, 0.5)
        val result = player.callPrivate("mix", listOf(long, short)) as DoubleArray
        assertEquals(listOf(1.5, 1.5, 1.0, 1.0), result.toList())
    }

    @Test
    fun `mix with a single channel returns an equivalent copy`() {
        val a = doubleArrayOf(0.1, -0.2, 0.3)
        val result = player.callPrivate("mix", listOf(a)) as DoubleArray
        assertEquals(a.toList(), result.toList())
    }

    @Test
    fun `mix with an empty channel list throws NoSuchElementException`() {
        // channels.maxOf { it.size } on an empty list has no default and throws --
        // this is the bug path hit by playSong() when a file has a header but no
        // channel lines at all.
        assertFailsWith<NoSuchElementException> { player.callPrivate("mix", emptyList<DoubleArray>()) }
    }

    @Test
    fun `mix with an all-empty-array channel list returns an empty array`() {
        val result = player.callPrivate("mix", listOf(DoubleArray(0), DoubleArray(0))) as DoubleArray
        assertTrue(result.isEmpty())
    }

    // ================= normalize =================

    @Test
    fun `normalize divides by peak when peak exceeds one`() {
        val samples = doubleArrayOf(2.0, -1.0, 0.5)
        val result = player.callPrivate("normalize", samples) as DoubleArray
        assertEquals(listOf(1.0, -0.5, 0.25), result.toList())
    }

    @Test
    fun `normalize leaves samples unchanged when already within range`() {
        val samples = doubleArrayOf(0.5, -0.3)
        val result = player.callPrivate("normalize", samples) as DoubleArray
        assertEquals(samples.toList(), result.toList())
    }

    @Test
    fun `normalize leaves an all-silent array unchanged rather than dividing by zero`() {
        val samples = doubleArrayOf(0.0, 0.0, 0.0)
        val result = player.callPrivate("normalize", samples) as DoubleArray
        assertEquals(listOf(0.0, 0.0, 0.0), result.toList())
    }

    @Test
    fun `normalize on an empty array returns an empty array`() {
        val result = player.callPrivate("normalize", DoubleArray(0)) as DoubleArray
        assertTrue(result.isEmpty())
    }

    @Test
    fun `normalize treats a value of exactly one as already in range`() {
        val samples = doubleArrayOf(1.0, -1.0, 0.2)
        val result = player.callPrivate("normalize", samples) as DoubleArray
        assertEquals(samples.toList(), result.toList()) // peak == 1.0 hits the <= 1.0 short-circuit
    }

    // ================= playSong: paths that don't reach play() =================

    @Test
    fun `playSong on a nonexistent file does nothing and does not throw`() {
        player.playSong("/no/such/path/song.txt")
    }

    @Test
    fun `playSong on an empty file does nothing and does not throw`() {
        val file = tempFile("")
        player.playSong(file.path)
    }

    @Test
    fun `playSong on a file with only blank lines fails with an invalid header error`() {
        val file = tempFile("\n\n   \n")
        assertFailsWith<IllegalArgumentException> { player.playSong(file.path) }
    }

    @Test
    fun `playSong propagates the header parse error for a malformed first line`() {
        val file = tempFile("not a valid header")
        assertFailsWith<IllegalArgumentException> { player.playSong(file.path) }
    }

    @Test
    fun `playSong with a header but no channel lines has nothing to play, but doesn't throw`() {
        // Reproduces the mix() empty-list bug end-to-end: a header-only file has
        // zero parsed channels, and mix(emptyList()) throws NoSuchElementException
        // before playSong ever reaches audio playback.
        val file = tempFile("100 4 60")
        player.playSong(file.path)
    }

    @Test
    fun `playSong with only unrecognized channel lines has nothing to play, but doesn't throw`() {
        // Every channel line fails to parse (unknown waveform), so channels ends up
        // empty after filtering out the nulls -- same empty-list bug as above.
        val file = tempFile("100 4 60\nkazoo | C4 1.0")
        player.playSong(file.path)
    }
}