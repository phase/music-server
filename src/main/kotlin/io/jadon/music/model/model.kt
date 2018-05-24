package io.jadon.music.model

/**
 * Helper function to clean up entity names.
 */
fun sanitize(name: String): String {
    // strip parens
    var result = name.replace("\\([^)]*\\)".toRegex(), "")
    result = result.toLowerCase()

    val specialChars = listOf(
            '!', '@', '#', '$', '%', '^', '&', '*',
            '(', ')', '-', '_', '+', '=', '{', '}',
            '[', ']', '\\', '|', ':', ';', '"', '\'',
            '<', '>', ',', '.', '?', '/', '`', '~'
    )

    specialChars.forEach {
        result = result.replace(it.toString(), "", true)
    }

    return result.trim()
}

interface Entity

// Alias for clarity
typealias SongId = Int

typealias ArtistId = Int
typealias AlbumId = Int
typealias UserId = Int
typealias PlaylistId = Int

// Content

data class Song(
        val id: SongId?,
        val name: String,
        val artistIds: List<ArtistId>,
        val isSingle: Boolean
) : Entity

data class Artist(
        val id: ArtistId?,
        val name: String
) : Entity

data class Album(
        val id: AlbumId?,
        val name: String,
        val artistIds: List<ArtistId>,
        val songIds: List<SongId>
) : Entity


// User content

data class User(
        val id: UserId?,
        val name: String,
        val passwordHash: String
) : Entity

data class Playlist(
        val id: PlaylistId?,
        val name: String,
        val userId: UserId,
        val songIds: List<SongId>
) : Entity
