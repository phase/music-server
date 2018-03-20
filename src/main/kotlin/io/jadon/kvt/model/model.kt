package io.jadon.kvt.model

data class Song(
        val name: String,
        val artistIds: List<Int>
)

data class Artist(
        val name: String
)

data class Album(
        val name: String,
        val artistIds: List<Int>,
        val songIds: List<Int>
)
