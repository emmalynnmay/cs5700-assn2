package org.example

interface Sound
{
    fun applyEffect(): DoubleArray
}

class Channel(private val waveformStrategy: WaveformStrategy) : Sound
{
    override fun applyEffect(): DoubleArray
    {
        // TODO: do nothing here?? what???
        return DoubleArray(0)
    }
}
