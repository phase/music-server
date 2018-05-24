package io.jadon.music.db

import io.jadon.music.MusicServer
import io.jadon.music.fs.UnprocessedSong
import io.jadon.music.model.*
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.WorkerExecutor
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.SchemaUtils
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
    val isSingle = bool("is_single")
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
    var isSingle by SongTable.isSingle

    fun asSong(): Song {
        return Song(id.value, name, artistIds, isSingle)
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
        val connectionString = "jdbc:postgresql://$host:$port/$database" +
                "?useUnicode=true" +
                "&useJDBCCompliantTimezoneShift=true" +
                "&useLegacyDatetimeCode=false" +
                "&serverTimezone=UTC" +
                "&nullNamePatternMatchesAll=true" +
                "&useSSL=false"
        org.jetbrains.exposed.sql.Database.connect(connectionString, driver = "org.postgresql.Driver",
                user = user, password = password)

        transaction {
            listOf(
                    SongTable,
                    ArtistTable,
                    PlaylistTable,
                    UserTable,
                    RecentEntityTable,
                    NewEntityTable
            ).forEach { SchemaUtils.create(it) }
        }
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

    override fun searchSongs(name: String): Future<List<Song>> {
        val future = Future.future<List<Song>>()
        executor.executeBlocking<List<Song>>({
            val songs = transaction {
                SongRow.all().filter {
                    sanitize(it.name).contains(sanitize(name))
                }
            }.map { it.asSong() }
            it.complete(songs)
            future.complete(songs)
        }, {})
        return future
    }

    override fun addSong(unprocessedSong: UnprocessedSong): Future<Song> {
        assert(unprocessedSong.name != null, { "Tried to add a song to the DB that doesn't have a name!" })
        val future = Future.future<Song>()
        executor.executeBlocking<Song?>({ o ->
            // check if song is already in db
            searchSongs(unprocessedSong.name!!).setHandler {
                val possibleDuplicates = it.result()
                for (song in possibleDuplicates) {
                    if (sanitize(unprocessedSong.name!!) == sanitize(song.name)) {
                        // song already exists in DB
                        o.complete(song)
                        future.complete(song)
                        return@setHandler
                    }
                }

                CompositeFuture.all(unprocessedSong.artists.mapIndexed { i, artistName ->
                    // search for artists already in the db
                    searchArtists(artistName).compose { Future.succeededFuture(Pair(i, it)) }
                }).setHandler {
                    val results = it.result().list<Pair<Int, List<Artist>>>().toMap()
                    // get the final list of artist ids
                    CompositeFuture.all(unprocessedSong.artists.mapIndexed { i, artistName ->
                        val artistIdFuture = Future.future<ArtistId>()
                        val possibleArtists = results[i]
                        if (possibleArtists != null && possibleArtists.isNotEmpty()) {
                            // there is an artist already in the db
                            artistIdFuture.complete(possibleArtists[0].id)
                        } else {
                            // we need to make a new artist in the db
                            addArtist(artistName).setHandler {
                                artistIdFuture.complete(it.result().id)
                            }
                        }
                        artistIdFuture
                    }).setHandler {
                        // add the song to the db
                        val finalArtistIds = it.result().list<ArtistId>()
                        val song = transaction {
                            SongRow.new {
                                this.name = unprocessedSong.name!!
                                this.artistIds = finalArtistIds
                                this.isSingle = unprocessedSong.isSingle
                            }.asSong()
                        }
                        o.complete(song)
                        future.complete(song)
                    }
                }
            }
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

    override fun searchArtists(name: String): Future<List<Artist>> {
        val future = Future.future<List<Artist>>()
        executor.executeBlocking<List<Artist>>({
            val artists = transaction {
                ArtistRow.all().filter {
                    sanitize(it.name).contains(sanitize(name))
                }
            }.map { it.asArtist() }
            it.complete(artists)
            future.complete(artists)
        }, {})
        return future
    }

    /**
     * Assumes the artist is not in the DB yet.
     * Use searchArtists() before using this method.
     */
    override fun addArtist(name: String): Future<Artist> {
        val future = Future.future<Artist>()
        executor.executeBlocking<Artist>({ o ->
            val artist = transaction {
                ArtistRow.new {
                    this.name = name
                }.asArtist()
            }
            o.complete(artist)
            future.complete(artist)
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

    override fun searchAlbums(name: String): Future<List<Album>> {
        val future = Future.future<List<Album>>()
        executor.executeBlocking<List<Album>>({
            val albums = transaction {
                AlbumRow.all().filter {
                    sanitize(it.name).contains(sanitize(name))
                }
            }.map { it.asAlbum() }
            it.complete(albums)
            future.complete(albums)
        }, {})
        return future
    }

    /**
     * Assumes the album is not in the DB yet.
     * Use searchAlbums() before using this method.
     */
    override fun addAlbum(name: String, artistIds: List<ArtistId>, songIds: List<SongId>): Future<Album> {
        val future = Future.future<Album>()
        executor.executeBlocking<Album>({ o ->
            val album = transaction {
                AlbumRow.new {
                    this.name = name
                    this.artistIds = artistIds
                    this.songIds = songIds
                }.asAlbum()
            }
            o.complete(album)
            future.complete(album)
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
