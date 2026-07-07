package org.example

import kotlin.math.PI

class Channel(private val waveformStrategy: WaveformStrategy, private val notes: List<Note>, private val header: SongHeader) : Sound()
{
    init 
    {
        println("Building channel samples!")
        samples = buildSamples()
    }

    fun getNoteStartSamples(): List<Int>
    {
        val starts = mutableListOf<Int>()
        var currentSample = 0

        for (note in notes)
        {
            starts.add(currentSample)
            val durationSeconds = calculateDurationSeconds(header.tempo, note.beats)
            val sampleCount = (header.sampleRate * durationSeconds).toInt()
            currentSample += sampleCount
        }

        return starts
    }

    private fun calculateDurationSeconds(tempo: Int, beats: Double): Double
    {
        val SECONDS_IN_MINUTE: Double = 60.0
        return (SECONDS_IN_MINUTE / tempo) * beats // FIXME: error handling??
    }

    private fun buildSamples(): DoubleArray
    {
        var samples = DoubleArray(0);
        for (note in notes)
        {
            val pitch = PianoNotes[note.pianoNote] ?: error("unknown note")   // FIXME: error handling??
            val durationSeconds = calculateDurationSeconds(header.tempo, note.beats)
            samples += generate(pitch, durationSeconds, header.sampleRate, waveformStrategy::doStrategy)
        }
        return samples
    }

    private fun generate(
        frequency: Double,
        durationSeconds: Double,
        sampleRate: Int,
        shape: (phase: Double) -> Double,
    ): DoubleArray {
        val sampleCount = (sampleRate * durationSeconds).toInt()
        val samples = DoubleArray(sampleCount)

        if (frequency == 0.0)
        {
            samples.fill(0.0)
            return samples
        }

        val phaseIncrement = 2.0 * PI * frequency / sampleRate   // radians per sample
        var phase = 0.0
        for (n in 0 until sampleCount) {
            samples[n] = shape(phase)                    // the shape decides the amplitude
            phase += phaseIncrement                      // advance by frequency's worth of phase
            if (phase >= 2.0 * PI) phase -= 2.0 * PI     // wrap to keep phase in [0, 2π)
        }
        return samples
    }
}
