package io.jadon.kvt.web

import io.jadon.kvt.fs.AbstractFileSystem
import io.jadon.kvt.model.SongId
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router

object WebFileService {

    fun init(router: Router, fileSystem: AbstractFileSystem) {
        router.route(HttpMethod.GET, "/download/song/:id").handler { routingContext ->
            routingContext.response().putHeader("content-type", "application/octet-stream")
            routingContext.response().isChunked = true

            val id: SongId? = routingContext.request().getParam("id").toIntOrNull()

            id?.let {
                // valid id
                val bytes = fileSystem.getSongBytes(it)
                bytes?.let {
                    routingContext.response().end(Buffer.buffer(it))
                    it
                } ?: routingContext.response().end()
                it
            } ?: routingContext.response().end()
        }

        router.route(HttpMethod.GET, "/download/artwork/:type/:id").handler { routingContext ->
            routingContext.response().putHeader("content-type", "application/octet-stream")
            routingContext.response().isChunked = true

            val id: SongId? = routingContext.request().getParam("id").toIntOrNull()
            val type = routingContext.request().getParam("type")

            id?.let {
                val bytes: ByteArray? = when (type) {
                    "song" -> fileSystem.getSongArtworkBytes(id)
                    "album" -> fileSystem.getAlbumArtworkBytes(id)
                    "playlist" -> fileSystem.getPlaylistArtworkBytes(id)
                    else -> null
                }
                bytes?.let {
                    routingContext.response().end(Buffer.buffer(it))
                    it
                } ?: routingContext.response().end()
                it
            } ?: routingContext.response().end()

        }
    }

}