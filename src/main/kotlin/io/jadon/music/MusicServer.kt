package io.jadon.music

import io.jadon.music.db.Database
import io.jadon.music.db.DummyDatabase
import io.jadon.music.db.PostgreSQLDatabase
import io.jadon.music.fs.FileSystem
import io.jadon.music.provider.LocalFileProvider
import io.jadon.music.provider.Provider
import io.jadon.music.web.WebFileService
import io.jadon.music.web.rest.RestApiV1
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import java.io.File

open class Config {
    var mainDirectory: String = ""
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

    lateinit var fileSystem: FileSystem
    lateinit var database: Database
    lateinit var vertx: Vertx
    lateinit var webServer: HttpServer
    val providers = mutableListOf<Provider>()

    @JvmStatic
    fun main(args: Array<String>) {
        fileSystem = FileSystem(config.mainDirectory)

        database = config.db?.let { db ->
            PostgreSQLDatabase(
                    db.host, db.port, db.database, db.user, db.password
            )
        } ?: DummyDatabase()

        vertx = Vertx.vertx()
        val router = Router.router(vertx)


        providers.add(
                LocalFileProvider(
                        config.mainDirectory.let { if (it.endsWith("/")) it else "$it/" } + "fresh",
                        fileSystem, database
                )
        )

        providers.forEach { it.start() }

        RestApiV1.init(router)
        WebFileService.init(router, fileSystem)

        webServer = vertx.createHttpServer()
        val port = 2345
        webServer.requestHandler(router::accept)
        webServer.listen(port)
        println("Server started @ http://localhost:$port")
    }

}
