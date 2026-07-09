package org.example

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/** Records every phase passed to it and returns a fixed constant amplitude. */
class RecordingStrategy(private val output: Double = 1.0) : WaveformStrategy {
    val recordedPhases = mutableListOf<Double>()
    override fun doStrategy(phase: Double): Double {
        recordedPhases.add(phase)
        return output
    }
}

/** Throws if invoked at all — used to prove silence/rest paths never call the shape function. */
class ThrowingStrategy : WaveformStrategy {
    override fun doStrategy(phase: Double): Double =
        throw AssertionError("shape function should not have been called")
}

class ChannelTest {

    private val tolerance = 1e-9

    // ================= Empty input =================

    @Test
    fun `empty notes list produces empty samples and empty note starts`() {
        val header = SongHeader(sampleRate = 44100, beatsPerMeasure = 4, tempo = 120)
        val channel = Channel(ThrowingStrategy(), emptyList(), header)
        assertTrue(channel.samples().isEmpty())
        assertTrue(channel.getNoteStartSamples().isEmpty())
    }

    // ================= Rest / unknown notes =================

    @Test
    fun `rest note produces silence and never invokes the shape function`() {
        val header = SongHeader(sampleRate = 100, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(Note(pianoNote = "-", beats = 1.0))
        // ThrowingStrategy would fail the test if the shape closure were ever called.
        val channel = Channel(ThrowingStrategy(), notes, header)
        val samples = channel.samples()
        assertEquals(100, samples.size) // 1 beat at tempo 60 = 1s * 100 samples/sec
        assertTrue(samples.all { it == 0.0 })
    }

    @Test
    fun `unknown note name defaults to silence via elvis fallback`() {
        val header = SongHeader(sampleRate = 50, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(Note(pianoNote = "NotARealNote", beats = 1.0))
        val channel = Channel(ThrowingStrategy(), notes, header)
        val samples = channel.samples()
        assertEquals(50, samples.size)
        assertTrue(samples.all { it == 0.0 })
    }

    @Test
    fun `empty string note name defaults to silence`() {
        val header = SongHeader(sampleRate = 20, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(Note(pianoNote = "", beats = 1.0))
        val channel = Channel(ThrowingStrategy(), notes, header)
        assertTrue(channel.samples().all { it == 0.0 })
    }

    // ================= Sample count math =================

    @Test
    fun `single note sample count matches sampleRate times duration`() {
        // tempo=60 -> 1 beat = 1 second; sampleRate=8 -> 8 samples
        val header = SongHeader(sampleRate = 8, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(Note(pianoNote = "A4", beats = 1.0))
        val channel = Channel(RecordingStrategy(), notes, header)
        assertEquals(8, channel.samples().size)
    }

    @Test
    fun `total samples equal sum of each note's individual duration`() {
        val header = SongHeader(sampleRate = 10, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(
            Note(pianoNote = "A4", beats = 1.0),   // 10 samples
            Note(pianoNote = "-", beats = 0.5),    // 5 samples
            Note(pianoNote = "C4", beats = 2.0)    // 20 samples
        )
        val channel = Channel(RecordingStrategy(), notes, header)
        assertEquals(35, channel.samples().size)
    }

    @Test
    fun `zero beats note contributes zero samples but does not throw`() {
        val header = SongHeader(sampleRate = 10, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(
            Note(pianoNote = "A4", beats = 0.0),
            Note(pianoNote = "A4", beats = 1.0)
        )
        val channel = Channel(RecordingStrategy(), notes, header)
        assertEquals(10, channel.samples().size) // only the second note contributes
    }

    // ================= getNoteStartSamples =================

    @Test
    fun `note starts accumulate cumulative sample offsets`() {
        val header = SongHeader(sampleRate = 10, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(
            Note(pianoNote = "A4", beats = 1.0),  // 10 samples: starts at 0
            Note(pianoNote = "A4", beats = 2.0),  // 20 samples: starts at 10
            Note(pianoNote = "A4", beats = 0.5)   // 5 samples:  starts at 30
        )
        val channel = Channel(RecordingStrategy(), notes, header)
        assertEquals(listOf(0, 10, 30), channel.getNoteStartSamples())
    }

    @Test
    fun `consecutive zero-beat notes share the same start offset`() {
        val header = SongHeader(sampleRate = 10, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(
            Note(pianoNote = "A4", beats = 1.0),  // starts at 0, 10 samples
            Note(pianoNote = "A4", beats = 0.0),  // starts at 10, 0 samples
            Note(pianoNote = "A4", beats = 0.0),  // starts at 10, 0 samples
            Note(pianoNote = "A4", beats = 1.0)   // starts at 10, 10 samples
        )
        val channel = Channel(RecordingStrategy(), notes, header)
        assertEquals(listOf(0, 10, 10, 10), channel.getNoteStartSamples())
    }

    @Test
    fun `single note note starts contains only offset zero`() {
        val header = SongHeader(sampleRate = 10, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(Note(pianoNote = "A4", beats = 1.0))
        val channel = Channel(RecordingStrategy(), notes, header)
        assertEquals(listOf(0), channel.getNoteStartSamples())
    }

    // ================= Phase generation correctness =================

    @Test
    fun `shape function receives phase starting at zero and wraps at two PI`() {
        // frequency 440 (A4), sampleRate 880 -> phaseIncrement = 2*PI*440/880 = PI exactly.
        // Recorded phase sequence (captured BEFORE increment/wrap each step): 0, PI, 0, PI, ...
        val header = SongHeader(sampleRate = 880, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(Note(pianoNote = "A4", beats = 1.0)) // 1s * 880 = 880 samples
        val strategy = RecordingStrategy()
        val channel = Channel(strategy, notes, header)

        assertEquals(880, channel.samples().size)
        assertEquals(880, strategy.recordedPhases.size)
        assertEquals(0.0, strategy.recordedPhases[0], tolerance)
        assertEquals(PI, strategy.recordedPhases[1], tolerance)
        assertEquals(0.0, strategy.recordedPhases[2], tolerance)
        assertEquals(PI, strategy.recordedPhases[3], tolerance)
    }

    @Test
    fun `shape function output is written directly into samples array`() {
        val header = SongHeader(sampleRate = 5, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(Note(pianoNote = "A4", beats = 1.0))
        val channel = Channel(RecordingStrategy(output = 0.42), notes, header)
        assertTrue(channel.samples().all { it == 0.42 })
    }

    // ================= Invalid / unexpected inputs =================

    @Test
    fun `negative beats produces negative sample count and throws`() {
        val header = SongHeader(sampleRate = 10, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(Note(pianoNote = "A4", beats = -1.0))
        assertFailsWith<NegativeArraySizeException> {
            Channel(RecordingStrategy(), notes, header)
        }
    }

    @Test
    fun `negative tempo flips duration sign and throws`() {
        val header = SongHeader(sampleRate = 10, beatsPerMeasure = 4, tempo = -60)
        val notes = listOf(Note(pianoNote = "A4", beats = 1.0))
        assertFailsWith<NegativeArraySizeException> {
            Channel(RecordingStrategy(), notes, header)
        }
    }

    @Test
    fun `tempo of zero with zero beats produces zero samples without dividing by zero crashing`() {
        // (60.0 / 0) * 0.0 = Infinity * 0.0 = NaN, and NaN.toInt() == 0 in Kotlin,
        // so this resolves to a harmless zero-length contribution rather than an exception.
        val header = SongHeader(sampleRate = 10, beatsPerMeasure = 4, tempo = 0)
        val notes = listOf(Note(pianoNote = "A4", beats = 0.0))
        val channel = Channel(RecordingStrategy(), notes, header)
        assertTrue(channel.samples().isEmpty())
        assertEquals(listOf(0), channel.getNoteStartSamples())
    }

    @Test
    fun `sampleRate of zero produces zero-length notes regardless of beats`() {
        val header = SongHeader(sampleRate = 0, beatsPerMeasure = 4, tempo = 60)
        val notes = listOf(Note(pianoNote = "A4", beats = 4.0))
        val channel = Channel(RecordingStrategy(), notes, header)
        assertTrue(channel.samples().isEmpty())
        assertEquals(listOf(0), channel.getNoteStartSamples())
    }

    @Test
    fun `many rest notes in sequence produce a single continuous silent block`() {
        val header = SongHeader(sampleRate = 10, beatsPerMeasure = 4, tempo = 60)
        val notes = List(5) { Note(pianoNote = "-", beats = 1.0) }
        val channel = Channel(ThrowingStrategy(), notes, header)
        assertEquals(50, channel.samples().size)
        assertTrue(channel.samples().all { it == 0.0 })
        assertEquals(listOf(0, 10, 20, 30, 40), channel.getNoteStartSamples())
    }
}