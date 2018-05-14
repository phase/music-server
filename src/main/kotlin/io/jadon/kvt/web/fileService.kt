package io.jadon.kvt.web

import io.jadon.kvt.fs.AbstractFileSystem
import io.jadon.kvt.model.SongId
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router

object WebFileService {

    fun init(router: Router, fileSystem: AbstractFileSystem) {
        router.route(HttpMethod.GET, "/download/:id").handler { routingContext ->
            routingContext.response().putHeader("content-type", "application/octet-stream")
            routingContext.response().isChunked = true

            val id: SongId? = routingContext.request().getParam("id").toIntOrNull()

            id?.let {
                // valid id
                val bytes = fileSystem.getSongBytes(it)
                bytes?.let {
                    routingContext.response().end(Buffer.buffer(it))
                    it
                } ?: run {
                    routingContext.response().end()
                }
                it
            } ?: run {
                // invalid id
                routingContext.response().end()
            }
        }
    }

}