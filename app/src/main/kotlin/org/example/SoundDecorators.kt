package org.example

abstract class SoundDecorator(protected val inputSound: Sound) : Sound() {}

class VolumeDecorator(inputSound: Sound, level: Double) : SoundDecorator(inputSound)
{
    init
    {
        // FIXME: remove logs
        println("Applying volume decorator")
        // println("We originally have ${inputSound.getSoundSamples()[100]}")
        samples = DoubleArray(inputSound.getSoundSamples().size) { inputSound.getSoundSamples()[it] * level }
        // println("We now have ${samples[100]}")
    }
}

class TanhDecorator(inputSound: Sound, drive: Double) : SoundDecorator(inputSound)
{
    init
    {
        println("Applying tanh decorator")
        val originalSamples = inputSound.getSoundSamples()
        samples = DoubleArray(originalSamples.size) { i ->
            kotlin.math.tanh(originalSamples[i] * drive)
        }
    }
}

class ClipDecorator(inputSound: Sound, threshold: Double) : SoundDecorator(inputSound)
{
    init
    {
        println("Applying clip decorator")
        val originalSamples = inputSound.getSoundSamples()
        samples = DoubleArray(originalSamples.size) { i ->
            originalSamples[i].coerceIn(-threshold, threshold)
        }
    }
}

class ADSDecorator(inputSound: Sound, attackEnd: Double, decayEnd: Double, sustain: Double, sampleRate: Int) : SoundDecorator(inputSound)
{
    init
    {
        // FIXME: this doesn't work- it needs to be applied individually for each note
        println("Applying ADS decorator")
        val originalSamples = inputSound.getSoundSamples()

        val attackEndSample = (attackEnd * sampleRate).toInt()
        val decayEndSample = (decayEnd * sampleRate).toInt()

        samples = DoubleArray(originalSamples.size) { i ->
            val envelope = when {
                i < attackEndSample -> {
                    // Ramp 0 -> 1 during attack
                    i.toDouble() / attackEndSample
                }
                i < decayEndSample -> {
                    // Ramp 1 -> sustain during decay
                    val decayProgress = (i - attackEndSample).toDouble() / (decayEndSample - attackEndSample)
                    1.0 - decayProgress * (1.0 - sustain)
                }
                else -> {
                    // Hold at sustain level
                    sustain
                }
            }
            originalSamples[i] * envelope
        }
    }
}
