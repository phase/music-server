package io.jadon.kvt.db

import io.jadon.kvt.model.Album
import io.jadon.kvt.model.Artist
import io.jadon.kvt.model.Song
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.jetbrains.exposed.sql.Database as SQLDatabase

interface Database {

    // Song functions

    fun getSong(id: Int): Future<Song?>
    fun getSongs(ids: List<Int>): Future<List<Song>>
    fun searchSongs(name: String): Future<List<Song>>

    // Artist functions

    fun getArtist(id: Int): Future<Artist?>
    fun getArtists(ids: List<Int>): Future<List<Artist>>
    fun searchArtists(name: String): Future<List<Artist>>

    // Album functions

    fun getAlbum(id: Int): Future<Album?>
    fun getAlbums(ids: List<Int>): Future<List<Album>>
    fun searchAlbums(name: String): Future<List<Album>>

}

class DummyDatabase : Database {

    private val threadPool = Executors.newFixedThreadPool(3)

    override fun getSong(id: Int): Future<Song?> {
        return threadPool.submit<Song?> { null }
    }

    override fun getSongs(ids: List<Int>): Future<List<Song>> {
        return threadPool.submit<List<Song>> { listOf() }
    }

    override fun searchSongs(name: String): Future<List<Song>> {
        return threadPool.submit<List<Song>> { listOf() }
    }

    override fun getArtist(id: Int): Future<Artist?> {
        return threadPool.submit<Artist?> { null }
    }

    override fun getArtists(ids: List<Int>): Future<List<Artist>> {
        return threadPool.submit<List<Artist>> { listOf() }
    }

    override fun searchArtists(name: String): Future<List<Artist>> {
        return threadPool.submit<List<Artist>> { listOf() }
    }

    override fun getAlbum(id: Int): Future<Album?> {
        return threadPool.submit<Album?> { null }
    }

    override fun getAlbums(ids: List<Int>): Future<List<Album>> {
        return threadPool.submit<List<Album>> { listOf() }
    }

    override fun searchAlbums(name: String): Future<List<Album>> {
        return threadPool.submit<List<Album>> { listOf() }
    }
}
