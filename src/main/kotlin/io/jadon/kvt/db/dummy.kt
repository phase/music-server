package io.jadon.kvt.db

import io.jadon.kvt.MusicServer
import io.jadon.kvt.model.*
import org.mindrot.jbcrypt.BCrypt
import io.vertx.core.Future
import io.vertx.core.WorkerExecutor
import java.util.*
import kotlin.math.roundToInt

class DummyDatabase : Database {

    private var artists = mutableListOf<Artist>()
    private var songs = mutableListOf<Song>()
    private var albums = mutableListOf<Album>()
    private var users = mutableListOf<User>()

    init {
        seed()
    }

    fun seed() {
        artists.clear()
        songs.clear()
        albums.clear()

        artists.addAll((0..10).map { Artist(it, generateName(1)) })
        songs.addAll(
                artists.subList(0, artists.size - 2).mapIndexed { index, _ ->
                    Song(index, generateName(), listOf(index, index + 1))
                }
        )

        albums.addAll((0..4).map {
            Album(
                    it,
                    generateName(),
                    artists.mapIndexed { index, _ -> index }.subList(it, it + 2),
                    songs.mapIndexed { index, _ -> index }.subList(it, it + 3)
            )
        })

        users.addAll((0..10).map {
            User(
                    it,
                    generateName(1),
                    BCrypt.hashpw(generateName(1), BCrypt.gensalt())
            )
        })

        println("DUMMY database DATA")
        println("Artists: $artists")
        println("Songs: $songs")
        println("Albums: $albums")
    }

    private fun generateName(maxWords: Int = 1 + (Math.random() * 3).toInt()): String {
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
        val lowercase = "abcdefghijklmnopqrstuvwxyz".toCharArray()

        fun generateWord(maxLength: Int = 3 + (Math.random() * 7).toInt()): String {
            val buffer = StringBuffer()
            val firstLetter = uppercase[(Math.random() * 25).roundToInt()]
            buffer.append(firstLetter)
            buffer.append((1..maxLength).map { lowercase[(Math.random() * 25).roundToInt()] }.joinToString(""))
            return buffer.toString()
        }

        return (1..maxWords).joinToString(separator = " ") { generateWord() }
    }

    private val executor: WorkerExecutor by lazy { MusicServer.vertx.createSharedWorkerExecutor("dummy_db", 4) }

    // I don't know how to supply my own future to finish with so there's a lot of hackiness

    override fun getSong(id: SongId): Future<Song?> {
        val future = Future.future<Song?>()
        executor.executeBlocking<Song?>({
            val song = songs.getOrNull(id)
            it.complete(song)
            future.complete(song)
        }, {})
        return future
    }

    override fun searchSongs(name: String): Future<List<SongId>> {
        val future = Future.future<List<SongId>>()
        executor.executeBlocking<List<SongId>>({
            val songIds = songs
                    .filter { it.name.contains(name, ignoreCase = true) }
                    .mapNotNull { it.id }
            it.complete(songIds)
            future.complete(songIds)
        }, {})
        return future
    }

    override fun getArtist(id: ArtistId): Future<Artist?> {
        val future = Future.future<Artist?>()
        executor.executeBlocking<Artist?>({
            val artist = artists.getOrNull(id)
            it.complete(artist)
            future.complete(artist)
        }, {})
        return future
    }

    override fun searchArtists(name: String): Future<List<ArtistId>> {
        val future = Future.future<List<ArtistId>>()
        executor.executeBlocking<List<ArtistId>>({
            val artistIds = artists
                    .filter { it.name.contains(name, ignoreCase = true) }
                    .mapNotNull { it.id }
            it.complete(artistIds)
            future.complete(artistIds)
        }, {})
        return future
    }

    override fun getAlbum(id: AlbumId): Future<Album?> {
        val future = Future.future<Album?>()
        executor.executeBlocking<Album?>({
            val album = albums.getOrNull(id)
            it.complete(album)
            future.complete(album)
        }, {})
        return future
    }

    override fun searchAlbums(name: String): Future<List<AlbumId>> {
        val future = Future.future<List<AlbumId>>()
        executor.executeBlocking<List<AlbumId>>({
            val albumIds = albums
                    .filter { it.name.contains(name, ignoreCase = true) }
                    .mapNotNull { it.id }
            it.complete(albumIds)
            future.complete(albumIds)
        }, {})
        return future
    }

    // User content

    override fun getUser(id: UserId): Future<User?> {
        return Future.succeededFuture(null)
    }

    override fun getPlaylist(id: PlaylistId): Future<Playlist?> {
        return Future.succeededFuture(null)
    }

    override fun getUserFromName(username: String): Future<User?> {
        return Future.succeededFuture(users.first())
    }

    override fun loginUser(user: User): Future<Token> {
        return Future.succeededFuture(UUID.randomUUID().toString())
    }

    override fun isValidToken(token: String): Future<Boolean> {
        println("Checking token $token")
        return Future.succeededFuture(true)
    }

    override fun getUser(token: String): Future<User?> {
        return Future.succeededFuture(users.first())
    }

    override fun getNewEntity(offset: Int): Future<Entity?> {
        return Future.succeededFuture(songs.getOrNull(offset))
    }

    override fun getRecentEntity(user: User, offset: Int): Future<Entity?> {
        return Future.succeededFuture(songs.getOrNull(offset))
    }

    override fun getNewEntityCount(): Future<Int> {
        return Future.succeededFuture(songs.size)
    }

    override fun getRecentEntityCount(user: User): Future<Int> {
        return Future.succeededFuture(songs.size)
    }

}
