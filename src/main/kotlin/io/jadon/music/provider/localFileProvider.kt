package io.jadon.music.provider

import com.mpatric.mp3agic.ID3v2
import com.mpatric.mp3agic.Mp3File
import io.jadon.music.db.Database
import io.jadon.music.fs.FileSystem
import io.jadon.music.fs.UnprocessedSong
import java.io.File

class LocalFileProvider(directoryPath: String, fileSystem: FileSystem, database: Database) : Provider(fileSystem, database) {

    private val directory = File(directoryPath.let {
        if (it.endsWith("/")) it else "$it/"
    })

    override fun collect() {
        println("LocalFileProvider: Starting Collection Loop in ${directory.absolutePath}")
        while (true) {
            directory.listFiles().forEach { file ->
                if (file.isDirectory) {
                    // add album
                    println("[LFP] Found Album  ${file.absolutePath}")
                } else if (file.isFile) {
                    // add song
                    println("[LFP] Found Single ${file.absolutePath}")
                    val songBytes = file.readBytes()
                    val mp3File = Mp3File(file)

                    if (mp3File.hasId3v2Tag() || mp3File.hasId3v1Tag()) {
                        val tag = mp3File.id3v2Tag ?: mp3File.id3v1Tag
                        var name = tag.title
                        var artists = mutableListOf(tag.artist)
                        val imageBytes = if (tag is ID3v2) tag.albumImage else null

                        print("[LFP] Ripped info from ${file.absolutePath}\n" +
                                "    File Name: ${file.name}\n" +
                                "    Name: $name\n" +
                                "    Artist: $artists\n" +
                                "Is this okay? [Y/n]")
                        val confirmation = readLine()
                        if (confirmation?.toLowerCase()?.contains("y") != true) {
                            print("Name: ")
                            name = readLine()
                            print("Artists (separated by '|'): ")
                            artists = readLine().orEmpty().split("|").toMutableList()
                        }
                        val unprocessedSong = UnprocessedSong(songBytes, imageBytes, name, artists)
                        addSong(unprocessedSong).setHandler {
                            file.delete()
                        }
                    } else {
                        println("[LFP] Couldn't rip info from ${file.absolutePath}")
                    }
                }
            }
            println("[LFP] Done processing songs. Sleeping for 30 seconds.")
            Thread.sleep(30_000)
        }
    }

}