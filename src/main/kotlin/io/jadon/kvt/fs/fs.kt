package io.jadon.kvt.fs

import io.jadon.kvt.model.SongId
import java.io.File

interface AbstractFileSystem {

    fun getSongBytes(id: SongId): ByteArray?

}

class DiskFileSystem(directoryPath: String) : AbstractFileSystem {

    private val directory = File(directoryPath)

    override fun getSongBytes(id: SongId): ByteArray? {
        // Assuming everything is mp3 for now
        return try {
            val songFile = directory.resolve(id.toString() + ".mp3")
            println("Reading file: ${songFile.absolutePath}")
            songFile.readBytes()
        } catch (e: Exception) {
            null
        }
    }

}
