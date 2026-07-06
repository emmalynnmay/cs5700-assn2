package org.example

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

interface WaveformStrategy 
{
    fun doStrategy(phase: Double): Double
}

class SineStrategy() : WaveformStrategy 
{
    override fun doStrategy(phase: Double): Double = sin(phase)
}

class SquareStrategy() : WaveformStrategy 
{
    override fun doStrategy(phase: Double): Double = if (phase < PI) 1.0 else -1.0
}

class SawtoothStrategy() : WaveformStrategy 
{
    override fun doStrategy(phase: Double): Double = phase / PI - 1.0     // 0 -> -1.0, 2π -> +1.0
}

class WhiteNoiseStrategy() : WaveformStrategy 
{
    override fun doStrategy(phase: Double): Double = Random.nextDouble(-1.0, 1.0)   // ignores phase entirely
}
