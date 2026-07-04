package org.example

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val filepath = args[0]
        AudioPlayer().playSong(filepath)
    }
}
