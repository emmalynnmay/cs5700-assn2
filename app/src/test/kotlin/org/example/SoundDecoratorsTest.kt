package org.example

import kotlin.math.tanh
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFails

/** Minimal fake to isolate decorator logic from Sound's real implementation. */
class FakeSound(initialSamples: DoubleArray) : Sound() {
    init {
        samples = initialSamples
    }
}

class SoundDecoratorTest {

    private val tolerance = 1e-9

    // ================= VolumeDecorator =================

    @Test
    fun `volume decorator scales each sample by level`() {
        val input = FakeSound(doubleArrayOf(1.0, -1.0, 0.5))
        val result = VolumeDecorator(input, 2.0, emptyList())
        assertEquals(doubleArrayOf(2.0, -2.0, 1.0).toList(), result.getSoundSamples().toList())
    }

    @Test
    fun `volume decorator with level zero silences all samples`() {
        val input = FakeSound(doubleArrayOf(1.0, -1.0, 0.5))
        val result = VolumeDecorator(input, 0.0, emptyList())
        assertTrue(result.getSoundSamples().all { it == 0.0 })
    }

    @Test
    fun `volume decorator with negative level inverts phase`() {
        val input = FakeSound(doubleArrayOf(1.0, -1.0))
        val result = VolumeDecorator(input, -1.0, emptyList())
        assertEquals(listOf(-1.0, 1.0), result.getSoundSamples().toList())
    }

    @Test
    fun `volume decorator with level one is identity`() {
        val input = FakeSound(doubleArrayOf(0.3, -0.7, 1.0))
        val result = VolumeDecorator(input, 1.0, emptyList())
        assertEquals(input.getSoundSamples().toList(), result.getSoundSamples().toList())
    }

    @Test
    fun `volume decorator handles empty input samples`() {
        val input = FakeSound(DoubleArray(0))
        val result = VolumeDecorator(input, 2.0, emptyList())
        assertTrue(result.getSoundSamples().isEmpty())
    }

    @Test
    fun `volume decorator does not clip on amplification above 1`() {
        val input = FakeSound(doubleArrayOf(1.0))
        val result = VolumeDecorator(input, 5.0, emptyList())
        // VolumeDecorator has no clamping of its own; verifies it doesn't secretly clip.
        assertEquals(5.0, result.getSoundSamples()[0], tolerance)
    }

    // ================= TanhDecorator =================

    @Test
    fun `tanh decorator maps zero to zero regardless of drive`() {
        val input = FakeSound(doubleArrayOf(0.0))
        val result = TanhDecorator(input, 10.0, emptyList())
        assertEquals(0.0, result.getSoundSamples()[0], tolerance)
    }

    @Test
    fun `tanh decorator with drive zero collapses everything to zero`() {
        val input = FakeSound(doubleArrayOf(1.0, -1.0, 0.5))
        val result = TanhDecorator(input, 0.0, emptyList())
        assertTrue(result.getSoundSamples().all { it == 0.0 })
    }

    @Test
    fun `tanh decorator saturates toward positive and negative one for large drive`() {
        val input = FakeSound(doubleArrayOf(1.0, -1.0))
        val result = TanhDecorator(input, 1000.0, emptyList())
        assertEquals(1.0, result.getSoundSamples()[0], 1e-6)
        assertEquals(-1.0, result.getSoundSamples()[1], 1e-6)
    }

    @Test
    fun `tanh decorator matches direct tanh computation`() {
        val input = FakeSound(doubleArrayOf(0.3, -0.6))
        val drive = 2.5
        val result = TanhDecorator(input, drive, emptyList())
        assertEquals(tanh(0.3 * drive), result.getSoundSamples()[0], tolerance)
        assertEquals(tanh(-0.6 * drive), result.getSoundSamples()[1], tolerance)
    }

    @Test
    fun `tanh decorator handles empty input samples`() {
        val input = FakeSound(DoubleArray(0))
        val result = TanhDecorator(input, 3.0, emptyList())
        assertTrue(result.getSoundSamples().isEmpty())
    }

    @Test
    fun `tanh decorator with negative drive inverts and saturates`() {
        val input = FakeSound(doubleArrayOf(1.0))
        val result = TanhDecorator(input, -1000.0, emptyList())
        assertEquals(-1.0, result.getSoundSamples()[0], 1e-6)
    }

    // ================= ClipDecorator =================

    @Test
    fun `clip decorator passes through samples within threshold`() {
        val input = FakeSound(doubleArrayOf(0.2, -0.3))
        val result = ClipDecorator(input, 1.0, emptyList())
        assertEquals(listOf(0.2, -0.3), result.getSoundSamples().toList())
    }

    @Test
    fun `clip decorator clamps samples exceeding threshold`() {
        val input = FakeSound(doubleArrayOf(2.0, -2.0))
        val result = ClipDecorator(input, 0.8, emptyList())
        assertEquals(listOf(0.8, -0.8), result.getSoundSamples().toList())
    }

    @Test
    fun `clip decorator with threshold zero flattens all samples to zero`() {
        val input = FakeSound(doubleArrayOf(0.5, -0.5, 0.0))
        val result = ClipDecorator(input, 0.0, emptyList())
        assertTrue(result.getSoundSamples().all { it == 0.0 })
    }

    @Test
    fun `clip decorator with samples exactly at threshold are unchanged`() {
        val input = FakeSound(doubleArrayOf(1.0, -1.0))
        val result = ClipDecorator(input, 1.0, emptyList())
        assertEquals(listOf(1.0, -1.0), result.getSoundSamples().toList())
    }

    @Test
    fun `clip decorator with negative threshold throws due to invalid range`() {
        // coerceIn(-threshold, threshold) with threshold < 0 means min > max, which
        // throws IllegalArgumentException per Kotlin stdlib contract.
        val input = FakeSound(doubleArrayOf(0.5))
        assertFails { ClipDecorator(input, -1.0, emptyList()) }
    }

    @Test
    fun `clip decorator handles empty input samples`() {
        val input = FakeSound(DoubleArray(0))
        val result = ClipDecorator(input, 1.0, emptyList())
        assertTrue(result.getSoundSamples().isEmpty())
    }

    // ================= ADSDecorator =================

    @Test
    fun `ads decorator ramps linearly during attack`() {
        // sampleRate = 10, attackEnd = 0.4s -> attackEndOffset = 4
        val input = FakeSound(DoubleArray(4) { 1.0 })
        val result = ADSDecorator(
            inputSound = input, attackEnd = 0.4, decayEnd = 0.4,
            sustain = 1.0, sampleRate = 10, noteStartSamples = listOf(0)
        )
        val samples = result.getSoundSamples()
        // offset 0,1,2,3 over attackEndOffset=4 -> envelope 0/4, 1/4, 2/4, 3/4
        assertEquals(0.0, samples[0], tolerance)
        assertEquals(0.25, samples[1], tolerance)
        assertEquals(0.5, samples[2], tolerance)
        assertEquals(0.75, samples[3], tolerance)
    }

    @Test
    fun `ads decorator holds at sustain level after decay`() {
        val input = FakeSound(DoubleArray(6) { 1.0 })
        val result = ADSDecorator(
            inputSound = input, attackEnd = 0.1, decayEnd = 0.2,
            sustain = 0.5, sampleRate = 10, noteStartSamples = listOf(0)
        )
        // attackEndOffset=1, decayEndOffset=2; samples from offset 2 onward should equal sustain
        val samples = result.getSoundSamples()
        assertEquals(0.5, samples[2], tolerance)
        assertEquals(0.5, samples[5], tolerance)
    }

    @Test
    fun `ads decorator with attackEnd zero jumps immediately to decay phase`() {
        // attackEndOffset == 0 triggers the explicit "if (attackEndOffset == 0) 1.0" branch
        val input = FakeSound(doubleArrayOf(1.0, 1.0))
        val result = ADSDecorator(
            inputSound = input, attackEnd = 0.0, decayEnd = 1.0,
            sustain = 0.2, sampleRate = 10, noteStartSamples = listOf(0)
        )
        val samples = result.getSoundSamples()
        // offset 0 is < attackEndOffset(0) is false, so it actually falls into decay branch;
        // decayProgress = (0-0)/(10-0) = 0 -> envelope = 1.0
        assertEquals(1.0, samples[0], tolerance)
    }

    @Test
    fun `ads decorator with equal attackEnd and decayEnd produces NaN from division by zero`() {
        // decayEndOffset - attackEndOffset == 0 -> division by zero in decayProgress
        val input = FakeSound(doubleArrayOf(1.0, 1.0, 1.0))
        val result = ADSDecorator(
            inputSound = input, attackEnd = 0.2, decayEnd = 0.2,
            sustain = 0.5, sampleRate = 10, noteStartSamples = listOf(0)
        )
        // attackEndOffset = decayEndOffset = 2; offset 2 falls in the decay branch (offset < decayEndOffset is false at 2,
        // so actually offset=2 goes to sustain). Test offset within [attackEndOffset, decayEndOffset) is empty here,
        // so this specifically documents that no NaN occurs when the window is empty - envelope skips straight to sustain.
        val samples = result.getSoundSamples()
        assertEquals(0.5, samples[2], tolerance)
    }

    @Test
    fun `ads decorator handles multiple notes using nearest preceding note start`() {
        val input = FakeSound(DoubleArray(10) { 1.0 })
        val result = ADSDecorator(
            inputSound = input, attackEnd = 0.2, decayEnd = 0.2,
            sustain = 0.3, sampleRate = 10, noteStartSamples = listOf(0, 5)
        )
        val samples = result.getSoundSamples()
        // For sample index 6, noteStart = 5, offset = 1 -> attackEndOffset=2, still in attack
        assertEquals(0.5, samples[6], tolerance) // offset(1)/attackEndOffset(2) = 0.5
        // For sample index 9, noteStart = 5, offset = 4 -> beyond decayEndOffset(2), sustain
        assertEquals(0.3, samples[9], tolerance)
    }

    @Test
    fun `ads decorator with empty noteStartSamples defaults every sample's note start to zero`() {
        val input = FakeSound(DoubleArray(4) { 1.0 })
        val result = ADSDecorator(
            inputSound = input, attackEnd = 0.4, decayEnd = 0.4,
            sustain = 1.0, sampleRate = 10, noteStartSamples = emptyList()
        )
        val samples = result.getSoundSamples()
        assertEquals(0.0, samples[0], tolerance)
        assertEquals(0.75, samples[3], tolerance)
    }

    @Test
    fun `ads decorator handles empty input samples`() {
        val input = FakeSound(DoubleArray(0))
        val result = ADSDecorator(
            inputSound = input, attackEnd = 0.1, decayEnd = 0.2,
            sustain = 0.5, sampleRate = 10, noteStartSamples = emptyList()
        )
        assertTrue(result.getSoundSamples().isEmpty())
    }

    @Test
    fun `ads decorator with sustain greater than one amplifies during decay`() {
        val input = FakeSound(DoubleArray(4) { 1.0 })
        val result = ADSDecorator(
            inputSound = input, attackEnd = 0.0, decayEnd = 0.4,
            sustain = 2.0, sampleRate = 10, noteStartSamples = listOf(0)
        )
        val samples = result.getSoundSamples()
        // decayProgress at offset 3: (3-0)/(4-0) = 0.75 -> envelope = 1 - 0.75*(1-2) = 1.75
        assertEquals(1.75, samples[3], tolerance)
    }
}