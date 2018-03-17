package io.jadon.kvt.db

import io.jadon.kvt.model.*
import java.util.concurrent.Future
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.Executors
import org.jetbrains.exposed.sql.Database as SQLDatabase

interface Database {

    fun getSong(id: Int): Future<Song?>
    fun getSongs(ids: List<Int>): Future<List<Song>>

    fun getArtist(id: Int): Future<Artist?>
    fun getArtists(ids: List<Int>): Future<List<Artist>>

    fun getAlbum(id: Int): Future<Album?>
    fun getAlbums(ids: List<Int>): Future<List<Album>>

}

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
        SQLDatabase.connect(connectionString, driver = "org.postgresql.Driver")
    }

    override fun getSong(id: Int): Future<Song?> {
        return threadPool.submit<Song?> {
            transaction {
                Song.findById(id)
            }
        }
    }

    override fun getSongs(ids: List<Int>): Future<List<Song>> {
        return threadPool.submit<List<Song>> {
            transaction {
                Song.find {
                    Songs.id inList ids
                }
            }.toList()
        }
    }

    override fun getArtist(id: Int): Future<Artist?> {
        return threadPool.submit<Artist?> {
            transaction {
                Artist.findById(id)
            }
        }
    }

    override fun getArtists(ids: List<Int>): Future<List<Artist>> {
        return threadPool.submit<List<Artist>> {
            transaction {
                Artist.find {
                    Artists.id inList ids
                }
            }.toList()
        }
    }

    override fun getAlbum(id: Int): Future<Album?> {
        return threadPool.submit<Album?> {
            transaction {
                Album.findById(id)
            }
        }
    }

    override fun getAlbums(ids: List<Int>): Future<List<Album>> {
        return threadPool.submit<List<Album>> {
            transaction {
                Album.find {
                    Albums.id inList ids
                }
            }.toList()
        }
    }

}
