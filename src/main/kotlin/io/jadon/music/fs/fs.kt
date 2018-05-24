package io.jadon.music.fs

import io.jadon.music.MusicServer
import io.jadon.music.model.AlbumId
import io.jadon.music.model.PlaylistId
import io.jadon.music.model.SongId
import io.vertx.core.buffer.Buffer
import java.io.File

class FileSystem(directoryPath: String) {

    private val directory = File(directoryPath.let {
        if (it.endsWith("/")) it else "$it/"
    })

    private fun readFile(file: File): ByteArray? {
        println("[FS] Reading file: ${file.absolutePath}")
        return try {
            MusicServer.vertx.fileSystem().readFileBlocking(file.absolutePath).byteBuf.array()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeFile(file: File, byteArray: ByteArray) {
        println("[FS] Writing File: ${file.absoluteFile} (${byteArray.size})")
        try {
            MusicServer.vertx.fileSystem().writeFile(file.absolutePath, Buffer.buffer(byteArray)) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

//    private val temp by lazy { readFile(directory.resolve("artwork/test.jpg")) }

    fun getSongBytes(id: SongId): ByteArray? {
        // Assuming everything is mp3 for now
        return readFile(directory.resolve("indexed/" + id.toString() + ".mp3"))
    }

    fun getSongArtworkBytes(id: SongId): ByteArray? {
        return readFile(directory.resolve("artwork/song/" + id.toString() + ".jpg"))
    }

    fun getAlbumArtworkBytes(id: AlbumId): ByteArray? {
        return readFile(directory.resolve("artwork/album/" + id.toString() + ".jpg"))
    }

    fun getPlaylistArtworkBytes(id: PlaylistId): ByteArray? {
        return readFile(directory.resolve("artwork/playlist/" + id.toString() + ".jpg"))
    }

    fun addSong(unprocessedSong: UnprocessedSong, id: SongId) {
        writeFile(directory.resolve("indexed/" + id.toString() + ".mp3"), unprocessedSong.songBytes)
        if (unprocessedSong.isSingle && unprocessedSong.imageBytes != null && unprocessedSong.imageBytes.isNotEmpty()) {
            writeFile(directory.resolve("artwork/song/" + id.toString() + ".jpg"), unprocessedSong.imageBytes)
        }
    }

}

class UnprocessedSong(val songBytes: ByteArray, val imageBytes: ByteArray?, val isSingle: Boolean) {
    var name: String? = null
    var artists: List<String> = mutableListOf()

    constructor(songBytes: ByteArray, imageBytes: ByteArray?, isSingle: Boolean, name: String, artists: List<String>)
            : this(songBytes, imageBytes, isSingle) {
        this.name = name
        this.artists = artists
    }
}
