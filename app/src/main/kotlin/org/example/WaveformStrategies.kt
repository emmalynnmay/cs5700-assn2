package org.example

interface WaveformStrategy 
{
    fun outputSamples(
        frequency: Double, 
        durationSeconds: Double, 
        sampleRate: Int, 
        shape: (phase: Double) -> Double
    ): DoubleArray
}

class SineStrategy() : WaveformStrategy 
{
    override fun outputSamples(
        frequency: Double, 
        durationSeconds: Double, 
        sampleRate: Int, 
        shape: (phase: Double) -> Double
    ): DoubleArray 
    {
        return DoubleArray(0)
    }
}

class SquareStrategy() : WaveformStrategy 
{
    override fun outputSamples(
        frequency: Double, 
        durationSeconds: Double, 
        sampleRate: Int, 
        shape: (phase: Double) -> Double
    ): DoubleArray 
    {
        return DoubleArray(0)
    }
}

class SawtoothStrategy() : WaveformStrategy 
{
    override fun outputSamples(
        frequency: Double, 
        durationSeconds: Double, 
        sampleRate: Int, 
        shape: (phase: Double) -> Double
    ): DoubleArray 
    {
        return DoubleArray(0)
    }
}

class WhiteNoiseStrategy() : WaveformStrategy 
{
    override fun outputSamples(
        frequency: Double, 
        durationSeconds: Double, 
        sampleRate: Int, 
        shape: (phase: Double) -> Double
    ): DoubleArray 
    {
        return DoubleArray(0)
    }
}
