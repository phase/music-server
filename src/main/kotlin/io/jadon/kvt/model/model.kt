package io.jadon.kvt.model

interface Entity

data class Song(
        val id: Int?,
        val name: String,
        val artistIds: List<Int>
) : Entity

data class Artist(
        val id: Int?,
        val name: String
) : Entity

data class Album(
        val id: Int?,
        val name: String,
        val artistIds: List<Int>,
        val songIds: List<Int>
) : Entity
