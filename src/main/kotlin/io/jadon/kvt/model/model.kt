package io.jadon.kvt.model

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

// tables

object Songs : IntIdTable() {
    val name = varchar("name", 64)
    val artistIds = text("artist_ids")
}

object Artists : IntIdTable() {
    val name = varchar("name", 64)
}

object Albums : IntIdTable() {
    val name = varchar("name", 64)
    val artistIds = text("artist_ids")
    val songIds = text("song_ids")
}

// objects

class Song(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Song>(Songs)

    var name = Songs.name
    var artistIds = Songs.artistIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").map { it.toIntOrNull() }.toTypedArray() }
    )
}

class Artist(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Artist>(Artists)

    var name = Artists.name
}

class Album(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Album>(Albums)

    var name = Albums.name
    var artistIds = Albums.artistIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").map { it.toIntOrNull() }.toTypedArray() }
    )
    var songIds = Albums.songIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").map { it.toIntOrNull() }.toTypedArray() }
    )
}
