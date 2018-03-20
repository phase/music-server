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
    fun searchSongs(name: String): Future<List<Int>>

    // Artist functions

    fun getArtist(id: Int): Future<Artist?>
    fun searchArtists(name: String): Future<List<Int>>

    // Album functions

    fun getAlbum(id: Int): Future<Album?>
    fun searchAlbums(name: String): Future<List<Int>>

}
