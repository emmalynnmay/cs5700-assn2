package org.example

import java.io.File
import kotlin.math.abs
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

data class SongHeader(
    val sampleRate: Int,
    val beatsPerMeasure: Int,
    val tempo: Int
)

class AudioPlayer {
    
    fun playSong(filepath: String)
    {
        val lines: List<String> = readFileLines(filepath)
        if (lines.isEmpty()) {
            println("No data to play")
            return
        }

        val header = parseHeader(lines[0])
        val channels = lines.drop(1)
            .filter { it.isNotBlank() }
            .map { parseChannel(it, header) }
        var channelSamples: List<DoubleArray> = emptyList<DoubleArray>()
        for (channel in channels)
        {
            if (channel != null)
            {
                channelSamples += channel.getSoundSamples()
            }
        }

        val mixed = normalize(mix(channelSamples))
        play(mixed, header.sampleRate)
    }

    private fun play(samples: DoubleArray, sampleRate: Int) {
        // 16-bit, mono, signed, little-endian PCM
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val line: SourceDataLine = AudioSystem.getSourceDataLine(format)
        line.open(format)
        line.start()

        // Convert each Double in [-1.0, 1.0] into two bytes (a 16-bit signed sample).
        val buffer = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val clamped = samples[i].coerceIn(-1.0, 1.0)
            val value = (clamped * Short.MAX_VALUE).toInt()
            buffer[i * 2]     = (value and 0xFF).toByte()          // low byte
            buffer[i * 2 + 1] = (value shr 8 and 0xFF).toByte()    // high byte
        }

        line.write(buffer, 0, buffer.size)
        line.drain()   // block until every sample has finished playing
        line.stop()
        line.close()
    }

    private fun mix(channels: List<DoubleArray>): DoubleArray {
        val length = channels.maxOf { it.size }
        val mixed = DoubleArray(length)
        for (channel in channels) {
            for (n in channel.indices) {
                mixed[n] += channel[n]
            }
        }
        return mixed
    }

    private fun normalize(samples: DoubleArray): DoubleArray {
        val peak = samples.maxOfOrNull { abs(it) } ?: 0.0
        if (peak == 0.0 || peak <= 1.0) return samples    // silent, or already in range
        return DoubleArray(samples.size) { samples[it] / peak }
    }

    private fun parseHeader(line: String): SongHeader {
        val tokens = line.trim().split(" ").filter { it.isNotEmpty() }
        if (tokens.size != 3) 
        {
            throw IllegalArgumentException(
                "Invalid header: expected 3 values (sampleRate beatsPerMeasure tempo), " +
                    "got ${tokens.size} in line: \"$line\""
            )
        }
        val sampleRate = tokens[0].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid sampleRate value: \"${tokens[0]}\"")
        val beatsPerMeasure = tokens[1].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid beatsPerMeasure value: \"${tokens[1]}\"")
        val tempo = tokens[2].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid tempo value: \"${tokens[2]}\"")

        if (sampleRate <= 0) {
            throw IllegalArgumentException("sampleRate must be positive, got $sampleRate")
        }
        if (beatsPerMeasure <= 0) {
            throw IllegalArgumentException("beatsPerMeasure must be positive, got $beatsPerMeasure")
        }
        if (tempo <= 0) {
            throw IllegalArgumentException("tempo must be positive, got $tempo")
        }

        return SongHeader(
            sampleRate = sampleRate,
            beatsPerMeasure = beatsPerMeasure,
            tempo = tempo
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

    private fun parseChannel(line: String, header: SongHeader) : Sound? 
    {
        // Trailing "|" produces a trailing empty segment, so drop blanks
        val segments = line.split("|").filter { it.isNotBlank() }

        val settingsTokens = segments[0].trim().split(" ").filter { it.isNotEmpty() }
        if (settingsTokens.isEmpty()) {
            throw IllegalArgumentException(
                "Invalid channel: settings segment is empty in line: \"$line\""
            )
        }

        val waveformStrategy = getWaveformStrategy(settingsTokens[0])
        if (waveformStrategy == null)
        {
            return null
        }

        val effects = settingsTokens.drop(1)

        val notes = try {
            segments.drop(1).flatMap { measure -> parseMeasure(measure) }
        } catch (e: Exception) {
            println("Invalid channel: failed to parse measures in line: \"$line\", ${e}")
            return null
        }

        val channel = Channel(waveformStrategy, notes, header)
        val noteStartSamples = channel.getNoteStartSamples()
        var sound: Sound = channel

        sound = try {
            processEffects(effects, sound, header, noteStartSamples)
        } catch (e: Exception) {
            println("Invalid channel: failed to apply effects $effects in line: \"$line\", ${e}")
            return null
        }

        return sound
    }

    private fun processEffects(effects: List<String>, channel: Sound, header: SongHeader, noteStartSamples: List<Int>) : Sound 
    {
        var sound: Sound = channel
        for (effect in effects) {
            val parts = effect.split("$")
            val name = parts[0]
            val params = parts.drop(1).map { it.toDouble() }

            when (name) {
                "vol" -> {
                    val level = params[0]
                    sound = VolumeDecorator(sound, level, noteStartSamples)
                }
                "ads" -> {
                    val attack = params[0]
                    val decay = params[1]
                    val sustain = params[2]
                    sound = ADSDecorator(sound, attack, decay, sustain, header.sampleRate, noteStartSamples)
                }
                "clip" -> {
                    val threshold = params[0]
                    sound = ClipDecorator(sound, threshold, noteStartSamples)
                }
                "tanh" -> {
                    val drive = params[0]
                    sound = TanhDecorator(sound, drive, noteStartSamples)
                }
                else -> {
                    println("Unknown effect is not being applied: $name")
                }
            }
        }
        return sound
    }

    private fun parseMeasure(measure: String): List<Note> {
        val tokens = measure.trim().split(" ").filter { it.isNotBlank() }

        if (tokens.size % 2 != 0) {
            throw IllegalArgumentException(
                "Invalid measure: expected note/duration pairs, but got an odd number " +
                    "of tokens (${tokens.size}) in measure: \"$measure\""
            )
        }

        return tokens.chunked(2).map { (pianoNote, beats) ->
            val beatsValue = beats.toDoubleOrNull()
                ?: throw IllegalArgumentException(
                    "Invalid duration \"$beats\" for note \"$pianoNote\" in measure: \"$measure\""
                )

            if (beatsValue <= 0.0) {
                throw IllegalArgumentException(
                    "Duration must be positive, got $beatsValue for note \"$pianoNote\" " +
                        "in measure: \"$measure\""
                )
            }

            Note(
                pianoNote = pianoNote,
                beats = beatsValue
            )
        }
    }

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