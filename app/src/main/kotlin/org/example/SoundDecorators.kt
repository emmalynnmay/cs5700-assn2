package org.example

abstract class SoundDecorator(protected val inputSound: Sound) : Sound()
{
}

class VolumeDecorator(inputSound: Sound, level: Double) : SoundDecorator(inputSound)
{
    init
    {
        println("Applying volume decorator")
        println("We originally have ${inputSound.getSoundSamples().size}")
        samples = inputSound.getSoundSamples()
        // TODO: actually apply effect
        println("We now have ${samples.size}")
    }
}

class TahnDecorator(inputSound: Sound, gain: Double) : SoundDecorator(inputSound)
{
    init
    {
        println("Applying tahn decorator")
        println("We originally have ${inputSound.getSoundSamples().size}")
        samples = inputSound.getSoundSamples()
        // TODO: actually apply effect
        println("We now have ${samples.size}")
    }
}

class ClipDecorator(inputSound: Sound, threshold: Double) : SoundDecorator(inputSound)
{
    init
    {
        println("Applying clip decorator")
        println("We originally have ${inputSound.getSoundSamples().size}")
        samples = inputSound.getSoundSamples()
        // TODO: actually apply effect
        println("We now have ${samples.size}")
    }
}

class ADSDecorator(inputSound: Sound, attack: Double, decay: Double, sustain: Double) : SoundDecorator(inputSound)
{
    init
    {
        println("Applying ADS decorator")
        println("We originally have ${inputSound.getSoundSamples().size}")
        samples = inputSound.getSoundSamples()
        // TODO: actually apply effect
        println("We now have ${samples.size}")
    }
}
