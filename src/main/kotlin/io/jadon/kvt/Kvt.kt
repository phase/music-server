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

object Kvt {

    lateinit var FS: AbstractFileSystem
    lateinit var DB: Database
    lateinit var VERTX: Vertx
    lateinit var SERVER: HttpServer

    @JvmStatic
    fun main(args: Array<String>) {
        FS = DiskFileSystem("D:/music/indexed/")
        DB = DummyDatabase()
        VERTX = Vertx.vertx()
        val router = Router.router(VERTX)

        RestApiV1.init(router)
        WebFileService.init(router, FS)

        SERVER = VERTX.createHttpServer()
        val port = 2345
        SERVER.requestHandler(router::accept)
        SERVER.listen(port)
        println("Server started @ http://localhost:$port")
    }

}
