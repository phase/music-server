package io.jadon.kvt

import io.jadon.kvt.db.Database
import io.jadon.kvt.db.DummyDatabase
import io.jadon.kvt.fs.AbstractFileSystem
import io.jadon.kvt.fs.DiskFileSystem
import io.jadon.kvt.web.WebFileService
import io.jadon.kvt.web.rest.RestApiV1
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router

object MusicServer {

    lateinit var fileSystem: AbstractFileSystem
    lateinit var database: Database
    lateinit var vertx: Vertx
    lateinit var webServer: HttpServer

    @JvmStatic
    fun main(args: Array<String>) {
        fileSystem = DiskFileSystem("D:/music/")
        database = DummyDatabase()
        vertx = Vertx.vertx()
        val router = Router.router(vertx)

        RestApiV1.init(router)
        WebFileService.init(router, fileSystem)

        webServer = vertx.createHttpServer()
        val port = 2345
        webServer.requestHandler(router::accept)
        webServer.listen(port)
        println("Server started @ http://localhost:$port")
    }

}
