package io.jadon.kvt.db

import io.jadon.kvt.model.Album
import io.jadon.kvt.model.Artist
import io.jadon.kvt.model.Song
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.roundToInt

class DummyDatabase : Database {

    private val threadPool = Executors.newFixedThreadPool(3)

    private var artists = mutableListOf<Artist>()
    private var songs = mutableListOf<Song>()
    private var albums = mutableListOf<Album>()

    init {
        seed()
    }

    fun seed() {
        artists.clear()
        songs.clear()
        albums.clear()

        artists.addAll((0..10).map { Artist(generateName(1)) })
        songs.addAll(
                artists.subList(0, artists.size - 2).mapIndexed { index, _ ->
                    Song(generateName(), listOf(index, index + 1))
                }
        )

        albums.addAll((0..4).map {
            Album(
                    generateName(),
                    artists.mapIndexed { index, _ -> index }.subList(it, it + 2),
                    songs.mapIndexed { index, _ -> index }.subList(it, it + 3)
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

    override fun getSong(id: Int): Future<Song?> {
        return threadPool.submit<Song?> { songs.getOrNull(id) }
    }

    override fun searchSongs(name: String): Future<List<Int>> {
        return threadPool.submit<List<Int>> { listOf() }
    }

    override fun getArtist(id: Int): Future<Artist?> {
        return threadPool.submit<Artist?> { artists.getOrNull(id) }
    }

    override fun searchArtists(name: String): Future<List<Int>> {
        return threadPool.submit<List<Int>> { listOf() }
    }

    override fun getAlbum(id: Int): Future<Album?> {
        return threadPool.submit<Album?> { albums.getOrNull(id) }
    }

    override fun searchAlbums(name: String): Future<List<Int>> {
        return threadPool.submit<List<Int>> { listOf() }
    }
}
