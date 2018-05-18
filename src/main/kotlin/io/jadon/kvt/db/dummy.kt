package io.jadon.kvt.db

import io.jadon.kvt.model.*
import org.mindrot.jbcrypt.BCrypt
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.roundToInt

class DummyDatabase : Database {

    private val threadPool = Executors.newFixedThreadPool(3)

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
                    generateName(),
                    BCrypt.hashpw(generateName(), BCrypt.gensalt())
            )
        })

        println("DUMMY DB DATA")
        println("Artists: $artists")
        println("Songs: $songs")
        println("Albums: $albums")
    }

    private fun generateName(maxWords: Int = 3): String {
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
        val lowercase = "abcdefghijklmnopqrstuvwxyz".toCharArray()

        fun generateWord(maxLength: Int = 10): String {
            val buffer = StringBuffer()
            val firstLetter = uppercase[(Math.random() * 25).roundToInt()]
            buffer.append(firstLetter)
            buffer.append((1..maxLength).map { lowercase[(Math.random() * 25).roundToInt()] }.joinToString(""))
            return buffer.toString()
        }

        return (1..maxWords).joinToString(separator = " ") { generateWord() }
    }

    override fun getSong(id: SongId): Future<Song?> {
        return threadPool.submit<Song?> { songs.getOrNull(id) }
    }

    override fun searchSongs(name: String): Future<List<SongId>> {
        return threadPool.submit<List<Int>> {
            songs
                    .filter { it.name.contains(name, ignoreCase = true) }
                    .mapNotNull { it.id }
        }
    }

    override fun getArtist(id: ArtistId): Future<Artist?> {
        return threadPool.submit<Artist?> { artists.getOrNull(id) }
    }

    override fun searchArtists(name: String): Future<List<ArtistId>> {
        return threadPool.submit<List<Int>> {
            artists
                    .filter { it.name.contains(name, ignoreCase = true) }
                    .mapNotNull { it.id }
        }
    }

    override fun getAlbum(id: AlbumId): Future<Album?> {
        return threadPool.submit<Album?> { albums.getOrNull(id) }
    }

    override fun searchAlbums(name: String): Future<List<AlbumId>> {
        return threadPool.submit<List<Int>> {
            albums
                    .filter { it.name.contains(name, ignoreCase = true) }
                    .mapNotNull { it.id }
        }
    }

    // User content

    override fun getUser(id: UserId): Future<User> {
        TODO("not implemented")
    }

    override fun getFavorites(id: UserId): Future<List<SongId>> {
        TODO("not implemented")
    }

    override fun getPlaylist(id: PlaylistId): Future<Playlist> {
        TODO("not implemented")
    }

    override fun getUserFromName(username: String): Future<User> {
        return threadPool.submit<User> { this.users.first() }
    }

    override fun loginUser(user: User): Future<Token> {
        return threadPool.submit<Token> { UUID.randomUUID().toString() }
    }

    override fun isValidToken(token: String): Future<Boolean> {
        println("Checking token $token")
        return threadPool.submit<Boolean> { true }
    }
}
