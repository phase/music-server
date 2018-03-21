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

    private var _name = SongTable.name
    val name: String
        get() = readValues[_name]

    private var _artistIds = SongTable.artistIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").mapNotNull { it.toIntOrNull() } }
    )
    val artistIds: List<Int>
        get() = _artistIds.toReal(readValues[_artistIds.column])

    fun asSong(): Song {
        return Song(id.value, name, artistIds)
    }
}

class ArtistRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ArtistRow>(ArtistTable)

    private var _name = ArtistTable.name
    val name: String
        get() = readValues[_name]

    fun asArtist(): Artist {
        return Artist(id.value, name)
    }
}

class AlbumRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AlbumRow>(AlbumTable)

    private var _name = AlbumTable.name
    val name: String
        get() = readValues[_name]

    private var _artistIds = AlbumTable.artistIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").mapNotNull { it.toIntOrNull() } }
    )
    val artistIds: List<Int>
        get() = _artistIds.toReal(readValues[_artistIds.column])

    private var _songIds = AlbumTable.songIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").mapNotNull { it.toIntOrNull() } }
    )
    val songIds: List<Int>
        get() = _songIds.toReal(readValues[_songIds.column])

    fun asAlbum(): Album {
        return Album(id.value, name, artistIds, songIds)
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

    override fun searchSongs(name: String): Future<List<Int>> {
        return threadPool.submit<List<Int>> {
            transaction {
                SongRow.find {
                    SongTable.name like name
                }
            }.toList().map { it.id.value }
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

    override fun searchArtists(name: String): Future<List<Int>> {
        return threadPool.submit<List<Int>> {
            transaction {
                ArtistRow.find {
                    ArtistTable.name like name
                }
            }.toList().map { it.id.value }
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

    override fun searchAlbums(name: String): Future<List<Int>> {
        return threadPool.submit<List<Int>> {
            transaction {
                AlbumRow.find {
                    AlbumTable.name like name
                }
            }.toList().map { it.id.value }
        }
    }

}
