package io.jadon.kvt.db

import io.jadon.kvt.model.*
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.Executors
import java.util.concurrent.Future

// tables

object SongTable : IntIdTable() {
    val name = varchar("name", 64)
    val artistIds = text("artist_ids")
}

object ArtistTable : IntIdTable() {
    val name = varchar("name", 64)
}

object AlbumTable : IntIdTable() {
    val name = varchar("name", 64)
    val artistIds = text("artist_ids")
    val songIds = text("song_ids")
}

// objects

class SongRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SongRow>(SongTable)

    var name = SongTable.name
    var artistIds = SongTable.artistIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").mapNotNull { it.toIntOrNull() } }
    )

    fun asSong(): Song {
        val name = readValues[name]
        val artistIds = artistIds.toReal(readValues[artistIds.column])
        return Song(name, artistIds)
    }
}

class ArtistRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ArtistRow>(ArtistTable)

    var name = ArtistTable.name

    fun asArtist(): Artist {
        val name = readValues[name]
        return Artist(name)
    }
}

class AlbumRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AlbumRow>(AlbumTable)

    var name = AlbumTable.name
    var artistIds = AlbumTable.artistIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").mapNotNull { it.toIntOrNull() } }
    )
    var songIds = AlbumTable.songIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").mapNotNull { it.toIntOrNull() } }
    )

    fun asAlbum(): Album {
        val name = readValues[name]
        val artistIds = artistIds.toReal(readValues[artistIds.column])
        val songIds = songIds.toReal(readValues[songIds.column])
        return Album(name, artistIds, songIds)
    }
}

// implementation

class PostgreSQLDatabase(
        host: String,
        port: Int = 5432,
        database: String,
        user: String,
        password: String
) : Database {

    private val threadPool = Executors.newFixedThreadPool(3)

    init {
        val connectionString = "jdbc:postgresql://$host:$port/$database?user=$user&password=$password"
        org.jetbrains.exposed.sql.Database.connect(connectionString, driver = "org.postgresql.Driver")
    }

    // Song functions

    override fun getSong(id: Int): Future<Song?> {
        return threadPool.submit<Song?> {
            transaction {
                SongRow.findById(id)
            }?.asSong()
        }
    }

    override fun getSongs(ids: List<Int>): Future<List<Song>> {
        return threadPool.submit<List<Song>> {
            transaction {
                SongRow.find {
                    SongTable.id inList ids
                }
            }.toList().map { it.asSong() }
        }
    }

    override fun searchSongs(name: String): Future<List<Song>> {
        return threadPool.submit<List<Song>> {
            transaction {
                SongRow.find {
                    SongTable.name like name
                }
            }.toList().map { it.asSong() }
        }
    }

    // Artist functions

    override fun getArtist(id: Int): Future<Artist?> {
        return threadPool.submit<Artist?> {
            transaction {
                ArtistRow.findById(id)
            }?.asArtist()
        }
    }

    override fun getArtists(ids: List<Int>): Future<List<Artist>> {
        return threadPool.submit<List<Artist>> {
            transaction {
                ArtistRow.find {
                    ArtistTable.id inList ids
                }
            }.toList().map { it.asArtist() }
        }
    }

    override fun searchArtists(name: String): Future<List<Artist>> {
        return threadPool.submit<List<Artist>> {
            transaction {
                ArtistRow.find {
                    ArtistTable.name like name
                }
            }.toList().map { it.asArtist() }
        }
    }

    // Album functions

    override fun getAlbum(id: Int): Future<Album?> {
        return threadPool.submit<Album?> {
            transaction {
                AlbumRow.findById(id)
            }?.asAlbum()
        }
    }

    override fun getAlbums(ids: List<Int>): Future<List<Album>> {
        return threadPool.submit<List<Album>> {
            transaction {
                AlbumRow.find {
                    AlbumTable.id inList ids
                }
            }.toList().map { it.asAlbum() }
        }
    }

    override fun searchAlbums(name: String): Future<List<Album>> {
        return threadPool.submit<List<Album>> {
            transaction {
                AlbumRow.find {
                    AlbumTable.name like name
                }
            }.toList().map { it.asAlbum() }
        }
    }

}
