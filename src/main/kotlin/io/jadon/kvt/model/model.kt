package io.jadon.kvt.model

interface Entity

data class Song(
        val name: String,
        val artistIds: List<Int>
) : Entity

data class Artist(
        val name: String
) : Entity

data class Album(
        val name: String,
        val artistIds: List<Int>,
        val songIds: List<Int>
) : Entity
