package org.example

abstract class SoundDecorator(private val inputSound: Sound) : Sound
{
}

class VolumeDecorator(inputSound: Sound) : SoundDecorator(inputSound)
{
    override fun applyEffect(): DoubleArray
    {
        println("Applying volume decorator")
        return DoubleArray(0)
    }
}

class TahnDecorator(inputSound: Sound) : SoundDecorator(inputSound)
{
    override fun applyEffect(): DoubleArray
    {
        println("Applying tahn decorator")
        return DoubleArray(0)
    }
}

class ClipDecorator(inputSound: Sound) : SoundDecorator(inputSound)
{
    override fun applyEffect(): DoubleArray
    {
        println("Applying clip decorator")
        return DoubleArray(0)
    }
}

class ADSDecorator(inputSound: Sound) : SoundDecorator(inputSound)
{
    override fun applyEffect(): DoubleArray
    {
        println("Applying ADS decorator")
        return DoubleArray(0)
    }
}
