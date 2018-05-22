package io.jadon.kvt.db

import io.jadon.kvt.model.*
import io.vertx.core.Future

typealias Token = String

interface Database {

    // Song functions

    fun getSong(id: SongId): Future<Song?>
    fun searchSongs(name: String): Future<List<SongId>>

    // Artist functions

    fun getArtist(id: ArtistId): Future<Artist?>
    fun searchArtists(name: String): Future<List<ArtistId>>

    // Album functions

    fun getAlbum(id: AlbumId): Future<Album?>
    fun searchAlbums(name: String): Future<List<AlbumId>>

    // User content

    fun getUser(id: UserId): Future<User?>
    fun getUser(token: String): Future<User?>
    fun getFavorites(id: UserId): Future<List<SongId>>
    fun getPlaylist(id: PlaylistId): Future<Playlist?>

    fun getUserFromName(username: String): Future<User?>

    /**
     * Invalidates old token (if it exists) and creates a new one
     */
    fun loginUser(user: User): Future<Token>

    fun isValidToken(token: String): Future<Boolean>

    fun getRecentEntityCount(user: User): Future<Int>
    fun getRecentEntity(user: User, offset: Int): Future<Entity?>

    fun getNewEntityCount(user: User): Future<Int>
    fun getNewEntity(user: User, offset: Int): Future<Entity?>

}
