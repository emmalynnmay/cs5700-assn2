package org.example

import kotlin.math.pow

object PianoNotes {
    val frequencies: Map<String, Double> = buildMap {
        val sharpNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val flatNames  = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

        for (key in 1..88) {
            val freq = 440.0 * 2.0.pow((key - 49) / 12.0)
            val semitonesFromC0 = key + 8
            val pitchClass = semitonesFromC0 % 12
            val octave = semitonesFromC0 / 12
            put("${sharpNames[pitchClass]}$octave", freq)
            if (flatNames[pitchClass] != sharpNames[pitchClass]) {
                put("${flatNames[pitchClass]}$octave", freq)
            }
        }
        put("-", 0.0)
    }

    operator fun get(note: String): Double? = frequencies[note]
}
