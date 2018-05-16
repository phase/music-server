package io.jadon.kvt.fs

import io.jadon.kvt.model.AlbumId
import io.jadon.kvt.model.PlaylistId
import io.jadon.kvt.model.SongId
import java.io.File

interface AbstractFileSystem {

    fun getSongBytes(id: SongId): ByteArray?

    // Artwork

    fun getSongArtworkBytes(id: SongId): ByteArray?

    fun getAlbumArtworkBytes(id: AlbumId): ByteArray?

    fun getPlaylistArtworkBytes(id: PlaylistId): ByteArray?

}

class DiskFileSystem(directoryPath: String) : AbstractFileSystem {

    private val directory = File(directoryPath.let {
        if (it.endsWith("/")) it else it + "/"
    })

    private fun readFile(file: File): ByteArray? {
        println("Reading file: ${file.absolutePath}")
        return try {
            file.readBytes()
        } catch (e: Exception) {
            null
        }
    }

    override fun getSongBytes(id: SongId): ByteArray? {
        // Assuming everything is mp3 for now
        return readFile(directory.resolve("indexed/" + id.toString() + ".mp3"))
    }

    override fun getSongArtworkBytes(id: SongId): ByteArray? {
        return readFile(directory.resolve("artwork/song/" + id.toString() + ".jpg"))
    }

    override fun getAlbumArtworkBytes(id: AlbumId): ByteArray? {
        return readFile(directory.resolve("artwork/album/" + id.toString() + ".jpg"))
    }

    override fun getPlaylistArtworkBytes(id: PlaylistId): ByteArray? {
        return readFile(directory.resolve("artwork/playlist/" + id.toString() + ".jpg"))
    }

}