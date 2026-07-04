package org.example

import java.io.File

data class SongHeader(
    val sampleRate: Int,
    val beatsPerMeasure: Int,
    val tempo: Int
)

class AudioPlayer {
    // val greeting: String
    //     get() {
    //         return "Hello World!"
    //     }
    // FIXME: remove this
    
    fun playSong(filepath: String)
    {
        println(filepath)
        val lines: List<String> = readFileLines(filepath)
        if (lines.isEmpty()) {
            println("No data to play")
            return
        }

        val header = parseHeader(lines[0])
        val channels = lines.drop(1)
            .filter { it.isNotBlank() }
            .map { parseChannel(it, header) }

        // val song = Song(header, channels)
        // TODO: use `song` to actually generate/play audio
    }

    private fun parseHeader(line: String): SongHeader {
        val tokens = line.trim().split(" ")
        return SongHeader(
            sampleRate = tokens[0].toInt(),
            beatsPerMeasure = tokens[1].toInt(),
            tempo = tokens[2].toInt()
        )
    }

    private fun getWaveformStrategy(name: String): WaveformStrategy?
    {
        when (name) 
        {
            "sin" -> return SineStrategy()
            "square" -> return SquareStrategy()
            "saw" -> return SawtoothStrategy()
            "whitenoise" -> return WhiteNoiseStrategy()
            else -> println("Error: Unknown waveform type!")
        }
        return null
    }

    private fun parseChannel(line: String, header: SongHeader) : Channel? {
        // Trailing "|" produces a trailing empty segment, so drop blanks
        val segments = line.split("|").filter { it.isNotBlank() }

        val settingsTokens = segments[0].trim().split(" ")
        val waveformStrategy = getWaveformStrategy(settingsTokens[0])
        if (waveformStrategy == null)
        {
            return null
        }
        val channel = Channel(waveformStrategy)

        val effects = settingsTokens.drop(1)
        println(effects)

        // FIXME: next up - note time????
        // val notes = segments.drop(1).flatMap { measure -> parseMeasure(measure, waveform) }
        // println(notes)

        // TODO: go back and apply effect decorators

        //return Channel(waveform, effects, notes)
        return channel
    }

    // private fun parseMeasure(measure: String, waveform: Waveform): List<Note> {
    //     val tokens = measure.trim().split(" ").filter { it.isNotBlank() }
    //     // tokens come in pairs: note duration note duration ...
    //     return tokens.chunked(2).map { (pitch, durationToken) ->
    //         Note(
    //             pitch = pitch,
    //             duration = durationToken.toDouble(),
    //             waveform = waveform
    //         )
    //     }
    // }

    private fun readFileLines(filePath: String): List<String>
    {
        return try {
            File(filePath).readLines()
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
            emptyList()
        }
    }
}