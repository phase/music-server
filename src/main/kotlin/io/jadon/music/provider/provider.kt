package io.jadon.music.provider

import io.jadon.music.db.Database
import io.jadon.music.fs.FileSystem
import io.jadon.music.fs.UnprocessedSong
import io.jadon.music.model.Song
import io.vertx.core.Future

abstract class Provider(val fileSystem: FileSystem, val database: Database) {

//    abstract fun search(query: String): List<ProviderResult /* json to send to the client */>

    fun start() {
        Thread {
            println("Starting Provider: ${this.javaClass.simpleName}")
            collect()
        }.start()
    }

    abstract fun collect()

    fun addSong(unprocessedSong: UnprocessedSong): Future<Song> {
        val future = Future.future<Song>()
        database.addSong(unprocessedSong).setHandler {
            if (it.succeeded()) {
                val song = it.result()
                fileSystem.addSong(unprocessedSong, song.id!!)
                future.complete(song)
            } else {
                future.fail(it.cause())
            }
        }
        return future
    }

}
