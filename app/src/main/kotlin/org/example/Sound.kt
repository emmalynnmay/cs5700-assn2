package org.example

abstract class Sound
{
    protected var samples: DoubleArray = DoubleArray(0)
     fun getSoundSamples(): DoubleArray = samples
}