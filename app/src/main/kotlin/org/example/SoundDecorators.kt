package org.example

abstract class SoundDecorator(protected val inputSound: Sound, protected val noteStartSamples: List<Int>) : Sound() {}

class VolumeDecorator(inputSound: Sound, level: Double, noteStartSamples: List<Int>) : SoundDecorator(inputSound, noteStartSamples)
{
    init
    {
        samples = DoubleArray(inputSound.getSoundSamples().size) { inputSound.getSoundSamples()[it] * level }
    }
}

class TanhDecorator(inputSound: Sound, drive: Double, noteStartSamples: List<Int>) : SoundDecorator(inputSound, noteStartSamples)
{
    init
    {
        val originalSamples = inputSound.getSoundSamples()
        samples = DoubleArray(originalSamples.size) { i ->
            kotlin.math.tanh(originalSamples[i] * drive)
        }
    }
}

class ClipDecorator(inputSound: Sound, threshold: Double, noteStartSamples: List<Int>) : SoundDecorator(inputSound, noteStartSamples)
{
    init
    {
        val originalSamples = inputSound.getSoundSamples()
        samples = DoubleArray(originalSamples.size) { i ->
            originalSamples[i].coerceIn(-threshold, threshold)
        }
    }
}

class ADSDecorator(
    inputSound: Sound, 
    attackEnd: Double, 
    decayEnd: Double, 
    sustain: Double, 
    sampleRate: Int, 
    noteStartSamples: List<Int>
) : SoundDecorator(inputSound, noteStartSamples)
{
    init
    {
        val originalSamples = inputSound.getSoundSamples()

        val attackEndOffset = (attackEnd * sampleRate).toInt()
        val decayEndOffset = (decayEnd * sampleRate).toInt()

        samples = DoubleArray(originalSamples.size) { i ->
            // Find which note this sample belongs to: the last note start <= i
            val noteStart = noteStartSamples.lastOrNull { it <= i } ?: 0
            val offset = i - noteStart

            val envelope = when {
                offset < attackEndOffset -> {
                    if (attackEndOffset == 0) 1.0 else offset.toDouble() / attackEndOffset
                }
                offset < decayEndOffset -> {
                    val decayProgress = (offset - attackEndOffset).toDouble() / (decayEndOffset - attackEndOffset)
                    1.0 - decayProgress * (1.0 - sustain)
                }
                else -> sustain
            }

            originalSamples[i] * envelope
        }
    }
}
