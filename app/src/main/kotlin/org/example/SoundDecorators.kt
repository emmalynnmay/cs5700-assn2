package org.example

abstract class SoundDecorator(protected val wrapped: Sound) : Sound

class VolumeDecorator(wrapped: Sound, private val level: Double) : SoundDecorator(wrapped) 
{
    override fun samples(): DoubleArray =
        wrapped.samples().map { it * level }.toDoubleArray()
}

class TanhDecorator(wrapped: Sound, private val drive: Double) : SoundDecorator(wrapped) 
{
    override fun samples(): DoubleArray
    {
        val originalSamples = wrapped.samples()
        return DoubleArray(originalSamples.size) { i ->
            kotlin.math.tanh(originalSamples[i] * drive)
        }
    }
}

class ClipDecorator(wrapped: Sound, private val threshold: Double) : SoundDecorator(wrapped) 
{
    init
    {
        if (threshold < 0)
        {
            throw IllegalArgumentException(
                "Threshold cannot be less than 0."
            )
        }
    }
    override fun samples(): DoubleArray
    {
        val originalSamples = wrapped.samples()
        return DoubleArray(originalSamples.size) { i ->
            originalSamples[i].coerceIn(-threshold, threshold)
        }
    }
}

class ADSDecorator(
    wrapped: Sound, 
    private val attackEnd: Double,
    private val decayEnd: Double,
    private val sustain: Double,
    private val sampleRate: Int,
    private val noteStartSamples: List<Int>
) : SoundDecorator(wrapped) 
{
    override fun samples(): DoubleArray
    {
        val originalSamples = wrapped.samples()

        val attackEndOffset = (attackEnd * sampleRate).toInt()
        val decayEndOffset = (decayEnd * sampleRate).toInt()

        return DoubleArray(originalSamples.size) { i ->
            // Find which note this sample belongs to: the last note start <= i
            val noteStart = noteStartSamples.lastOrNull { it <= i } ?: 0
            val offset = i - noteStart

            val envelope = when {
                offset < attackEndOffset -> {
                    if (attackEndOffset == 0) 1.0 else offset.toDouble() / attackEndOffset
                }
                offset < decayEndOffset -> {
                    if (attackEndOffset == decayEndOffset)
                    {
                        sustain
                    }
                    else
                    {
                        val decayProgress = (offset - attackEndOffset).toDouble() / (decayEndOffset - attackEndOffset)
                        1.0 - decayProgress * (1.0 - sustain)
                    }
                }
                else -> sustain
            }

            originalSamples[i] * envelope
        }
    }
}
