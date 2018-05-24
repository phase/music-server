package io.jadon.music.db

import io.jadon.music.fs.UnprocessedSong
import io.jadon.music.model.*
import io.vertx.core.Future

typealias Token = String

interface Database {

    // Song functions

    fun getSong(id: SongId): Future<Song?>
    fun searchSongs(name: String): Future<List<Song>>
    fun addSong(unprocessedSong: UnprocessedSong): Future<Song>

    // Artist functions

    fun getArtist(id: ArtistId): Future<Artist?>
    fun searchArtists(name: String): Future<List<Artist>>
    fun addArtist(name: String): Future<Artist>

    // Album functions

    fun getAlbum(id: AlbumId): Future<Album?>
    fun searchAlbums(name: String): Future<List<Album>>
    fun addAlbum(name: String, artistIds: List<ArtistId>, songIds: List<SongId>): Future<Album>

    // User content

    fun getUser(id: UserId): Future<User?>
    fun getUser(token: String): Future<User?>
    fun getPlaylist(id: PlaylistId): Future<Playlist?>

    fun getUserFromName(username: String): Future<User?>

    /**
     * Invalidates old token (if it exists) and creates a new one
     */
    fun loginUser(user: User): Future<Token>

    fun isValidToken(token: String): Future<Boolean>

    fun getRecentEntityCount(user: User): Future<Int>
    fun getRecentEntity(user: User, offset: Int): Future<Entity?>
    fun addRecentEntity(user: User, entity: Entity): Future<Void>

    fun getNewEntityCount(): Future<Int>
    fun getNewEntity(offset: Int): Future<Entity?>
    fun addNewEntity(entity: Entity): Future<Void>

}
