package io.jadon.kvt.db

import io.jadon.kvt.MusicServer
import io.jadon.kvt.model.*
import io.vertx.core.Future
import io.vertx.core.WorkerExecutor
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

internal enum class MusicEntityID(val id: Int) {
    SONG(0), ALBUM(1), PLAYLIST(2);

    companion object {
        fun valueOf(id: Int): MusicEntityID {
            return when (id) {
                0 -> SONG
                1 -> ALBUM
                2 -> PLAYLIST
                else -> throw IllegalArgumentException("$id is not a MusicEntityID")
            }
        }
    }
}

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

object PlaylistTable : IntIdTable() {
    val name = varchar("name", 64)
    val userId = integer("user_id")
    val songIds = text("song_ids")
}

object UserTable : IntIdTable() {
    val name = varchar("name", 64)
    val passwordHash = varchar("password_hash", 64)
    val token = varchar("token", 128)
}

object RecentEntityTable : IntIdTable() {
    val userId = integer("user_id")
    val time = datetime("timestamp")
    val type = integer("type")
    val entityId = integer("entity_id")
}

object NewEntityTable : IntIdTable() {
    val time = datetime("timestamp")
    val type = integer("type")
    val entityId = integer("entity_id")
}

// objects

class SongRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SongRow>(SongTable)

    var name by SongTable.name
    var artistIds by SongTable.artistIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").mapNotNull { it.toIntOrNull() } }
    )

    fun asSong(): Song {
        return Song(id.value, name, artistIds)
    }
}

class ArtistRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ArtistRow>(ArtistTable)

    var name by ArtistTable.name

    fun asArtist(): Artist {
        return Artist(id.value, name)
    }
}

class AlbumRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AlbumRow>(AlbumTable)

    var name by AlbumTable.name
    var artistIds by AlbumTable.artistIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").mapNotNull { it.toIntOrNull() } }
    )
    var songIds by AlbumTable.songIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").mapNotNull { it.toIntOrNull() } }
    )

    fun asAlbum(): Album {
        return Album(id.value, name, artistIds, songIds)
    }
}

class PlaylistRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlaylistRow>(PlaylistTable)

    var name by PlaylistTable.name
    var userId by PlaylistTable.userId
    var songIds by PlaylistTable.songIds.transform(
            { a -> a.joinToString(":") },
            { s -> s.split(":").mapNotNull { it.toIntOrNull() } }
    )

    fun asPlaylist(): Playlist {
        return Playlist(id.value, name, userId, songIds)
    }
}

class UserRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserRow>(UserTable)

    var name by UserTable.name
    var passwordHash by UserTable.passwordHash
    var token by UserTable.token

    fun asUser(): User {
        return User(id.value, name, passwordHash)
    }
}

class RecentEntityRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RecentEntityRow>(RecentEntityTable)

    var userId by RecentEntityTable.userId
    var time by RecentEntityTable.time
    var typeId by RecentEntityTable.type
    var entityId by RecentEntityTable.entityId

    internal fun getType(): MusicEntityID {
        return MusicEntityID.valueOf(typeId)
    }
}

class NewEntityRow(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<NewEntityRow>(NewEntityTable)

    var time by NewEntityTable.time
    var typeId by NewEntityTable.type
    var entityId by NewEntityTable.entityId

    internal fun getType(): MusicEntityID {
        return MusicEntityID.valueOf(typeId)
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

    private val executor: WorkerExecutor by lazy { MusicServer.vertx.createSharedWorkerExecutor("sql_db", 4) }

    init {
        val connectionString = "jdbc:postgresql://$host:$port/$database?user=$user&password=$password"
        org.jetbrains.exposed.sql.Database.connect(connectionString, driver = "org.postgresql.Driver")
    }

    // Song functions

    override fun getSong(id: SongId): Future<Song?> {
        val future = Future.future<Song?>()
        executor.executeBlocking<Song?>({
            val song = transaction {
                SongRow.findById(id)
            }?.asSong()
            it.complete(song)
            future.complete(song)
        }, {})
        return future
    }

    override fun searchSongs(name: String): Future<List<SongId>> {
        val future = Future.future<List<SongId>>()
        executor.executeBlocking<List<SongId>>({
            val songIds = transaction {
                SongRow.find {
                    SongTable.name like name
                }
            }.toList().map { it.id.value }
            it.complete(songIds)
            future.complete(songIds)
        }, {})
        return future
    }

    // Artist functions

    override fun getArtist(id: ArtistId): Future<Artist?> {
        val future = Future.future<Artist?>()
        executor.executeBlocking<Artist?>({
            val artist = transaction {
                ArtistRow.findById(id)
            }?.asArtist()
            it.complete(artist)
            future.complete(artist)
        }, {})
        return future
    }

    override fun searchArtists(name: String): Future<List<ArtistId>> {
        val future = Future.future<List<ArtistId>>()
        executor.executeBlocking<List<ArtistId>>({
            val artistIds = transaction {
                ArtistRow.find {
                    ArtistTable.name like name
                }
            }.toList().map { it.id.value }
            it.complete(artistIds)
            future.complete(artistIds)
        }, {})
        return future
    }

    // Album functions

    override fun getAlbum(id: AlbumId): Future<Album?> {
        val future = Future.future<Album?>()
        executor.executeBlocking<Album?>({
            val album = transaction {
                AlbumRow.findById(id)
            }?.asAlbum()
            it.complete(album)
            future.complete(album)
        }, {})
        return future
    }

    override fun searchAlbums(name: String): Future<List<AlbumId>> {
        val future = Future.future<List<AlbumId>>()
        executor.executeBlocking<List<AlbumId>>({
            val albumIds = transaction {
                AlbumRow.find {
                    AlbumTable.name like name
                }
            }.toList().map { it.id.value }
            it.complete(albumIds)
            future.complete(albumIds)
        }, {})
        return future
    }

    // User content

    override fun getUser(id: UserId): Future<User?> {
        val future = Future.future<User?>()
        executor.executeBlocking<User?>({
            val user = transaction {
                UserRow.findById(id)
            }?.asUser()
            it.complete(user)
            future.complete(user)
        }, {})
        return future
    }

    override fun getPlaylist(id: PlaylistId): Future<Playlist?> {
        val future = Future.future<Playlist?>()
        executor.executeBlocking<Playlist?>({
            val playlist = transaction {
                PlaylistRow.findById(id)
            }?.asPlaylist()
            it.complete(playlist)
            future.complete(playlist)
        }, {})
        return future
    }

    override fun getUserFromName(username: String): Future<User?> {
        val future = Future.future<User?>()
        executor.executeBlocking<User?>({
            val user = transaction {
                UserRow.find {
                    UserTable.name like username
                }.limit(1).firstOrNull()
            }?.asUser()
            it.complete(user)
            future.complete(user)
        }, {})
        return future
    }

    /*
     * Remove last token and generate a new one
     */
    override fun loginUser(user: User): Future<Token> {
        val future = Future.future<Token>()
        executor.executeBlocking<Token>({
            val token = UUID.randomUUID().toString()
            transaction {
                UserRow.findById(user.id!!)!!.token = token
            }
            it.complete(token)
            future.complete(token)
        }, {})
        return future
    }

    override fun isValidToken(token: String): Future<Boolean> {
        val future = Future.future<Boolean>()
        executor.executeBlocking<Boolean>({
            val result = transaction {
                UserRow.find { UserTable.token eq token }.limit(1).count() > 0
            }
            it.complete(result)
            future.complete(result)
        }, {})
        return future
    }

    override fun getUser(token: String): Future<User?> {
        val future = Future.future<User?>()
        executor.executeBlocking<User?>({
            val result = transaction {
                UserRow.find { UserTable.token eq token }.limit(1).firstOrNull()?.asUser()
            }
            it.complete(result)
            future.complete(result)
        }, {})
        return future
    }

    override fun getNewEntity(offset: Int): Future<Entity?> {
        val future = Future.future<Entity?>()
        executor.executeBlocking<Entity?>({ o ->
            val result = transaction {
                val entity = NewEntityRow.all().sortedBy { it.time }[offset]
                when (entity.getType()) {
                    MusicEntityID.SONG -> getSong(entity.entityId)
                    MusicEntityID.ALBUM -> getAlbum(entity.entityId)
                    MusicEntityID.PLAYLIST -> getPlaylist(entity.entityId)
                }
            }
            result.setHandler {
                val r = it.result()
                o.complete(r)
                future.complete(r)
            }
        }, {})
        return future
    }

    override fun getRecentEntity(user: User, offset: Int): Future<Entity?> {
        val future = Future.future<Entity?>()
        executor.executeBlocking<Entity?>({ o ->
            val result = transaction {
                val entity = RecentEntityRow.all().filter { it.userId == user.id }.sortedBy { it.time }[offset]
                when (entity.getType()) {
                    MusicEntityID.SONG -> getSong(entity.entityId)
                    MusicEntityID.ALBUM -> getAlbum(entity.entityId)
                    MusicEntityID.PLAYLIST -> getPlaylist(entity.entityId)
                }
            }
            result.setHandler {
                val r = it.result()
                o.complete(r)
                future.complete(r)
            }
        }, {})
        return future
    }

    override fun getNewEntityCount(): Future<Int> {
        val future = Future.future<Int>()
        executor.executeBlocking<Int>({
            val result = transaction {
                Math.max(100, NewEntityRow.all().count())
            }
            it.complete(result)
            future.complete(result)
        }, {})
        return future
    }

    override fun getRecentEntityCount(user: User): Future<Int> {
        val future = Future.future<Int>()
        executor.executeBlocking<Int>({
            val result = transaction {
                Math.max(100, RecentEntityRow.all().filter { it.userId == user.id }.count())
            }
            it.complete(result)
            future.complete(result)
        }, {})
        return future
    }

}
