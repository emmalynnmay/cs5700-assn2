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
        var channelSamples: List<DoubleArray> = emptyList<DoubleArray>()
        for (channel in channels)
        {
            if (channel != null)
            {
                channelSamples += channel.getSoundSamples()
            }
        }

        println("We have this many channel samples: ${channelSamples.size}")
        val mixed = normalize(mix(channelSamples))
        play(mixed, header.sampleRate)
    }

    fun play(samples: DoubleArray, sampleRate: Int) {
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

    private fun parseChannel(line: String, header: SongHeader) : Sound? 
    {
        println("-------- Parsing a new channel! --------")
        // Trailing "|" produces a trailing empty segment, so drop blanks
        val segments = line.split("|").filter { it.isNotBlank() }

        val settingsTokens = segments[0].trim().split(" ")
        val waveformStrategy = getWaveformStrategy(settingsTokens[0])
        if (waveformStrategy == null)
        {
            return null
        }

        val effects = settingsTokens.drop(1)

        val notes = segments.drop(1).flatMap { measure -> parseMeasure(measure) }

        var channel: Sound = Channel(waveformStrategy, notes, header)

        println(effects)
        channel = processEffects(effects, channel, header)

        return channel
    }

    private fun processEffects(effects: List<String>, channel: Sound, header: SongHeader) : Sound 
    {
        var sound: Sound = channel
        for (effect in effects) {
            val parts = effect.split("$")
            val name = parts[0]
            val params = parts.drop(1).map { it.toDouble() }

            when (name) {
                "vol" -> {
                    val level = params[0]
                    println("Volume effect: level=$level")
                    sound = VolumeDecorator(channel, level)
                }
                "ads" -> {
                    val attack = params[0]
                    val decay = params[1]
                    val sustain = params[2]
                    println("ADS effect: attack=$attack, decay=$decay, sustain=$sustain")
                    sound = ADSDecorator(channel, attack, decay, sustain, header.sampleRate)
                }
                "clip" -> {
                    val threshold = params[0]
                    println("Clip effect: threshold=$threshold")
                    sound = ClipDecorator(channel, threshold)
                }
                "tanh" -> {
                    val drive = params[0]
                    println("Tanh effect: drive=$drive")
                    sound = TanhDecorator(channel, drive)
                }
                else -> {
                    println("Unknown effect: $name")
                }
            }
        }
        return sound
    }

    private fun parseMeasure(measure: String): List<Note> {
        val tokens = measure.trim().split(" ").filter { it.isNotBlank() }
        // tokens come in pairs: note duration note duration ...
        return tokens.chunked(2).map { (pianoNote, beats) ->
            Note(
                pianoNote = pianoNote,
                beats = beats.toDouble()
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