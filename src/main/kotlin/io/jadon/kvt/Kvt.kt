package io.jadon.kvt

import io.jadon.kvt.db.Database
import io.jadon.kvt.db.DummyDatabase
import io.jadon.kvt.web.rest.RestApiV1
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer

object Kvt {

    lateinit var DB: Database
    lateinit var VERTX: Vertx
    lateinit var SERVER: HttpServer

    @JvmStatic
    fun main(args: Array<String>) {
        DB = DummyDatabase()
        VERTX = Vertx.vertx()
        val apiRouter = RestApiV1.init()

        SERVER = VERTX.createHttpServer()
        val port = 2345
        SERVER.requestHandler {
            when (true) {
                it.path().startsWith("/api") -> apiRouter.accept(it)
                it.path().startsWith("/download") -> {
                }
            }
        }
        SERVER.listen(port)
        println("Server started @ http://localhost:$port")
    }

}
