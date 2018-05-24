package io.jadon.music

import io.jadon.music.db.Database
import io.jadon.music.db.DummyDatabase
import io.jadon.music.db.PostgreSQLDatabase
import io.jadon.music.fs.AbstractFileSystem
import io.jadon.music.fs.DiskFileSystem
import io.jadon.music.web.WebFileService
import io.jadon.music.web.rest.RestApiV1
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.json.Json
import io.vertx.ext.web.Router

open class Config {
    var db: ConfigDatabase? = null

    open class ConfigDatabase {
        var host: String = ""
        var port: Int = 5432
        var database: String = ""
        var user: String = ""
        var password: String = ""
    }
}

object MusicServer {

    var config: Config = Json.decodeValue(
            MusicServer::class.java.getResource("/config.json").readText(),
            Config::class.java
    )

    lateinit var fileSystem: AbstractFileSystem
    lateinit var database: Database
    lateinit var vertx: Vertx
    lateinit var webServer: HttpServer

    @JvmStatic
    fun main(args: Array<String>) {
        fileSystem = DiskFileSystem("D:/music/")

        database = config.db?.let { db ->
            PostgreSQLDatabase(
                    db.host, db.port, db.database, db.user, db.password
            )
        } ?: DummyDatabase()

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
