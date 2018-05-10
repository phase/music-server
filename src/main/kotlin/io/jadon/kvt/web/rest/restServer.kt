package io.jadon.kvt.web.rest

import io.jadon.kvt.Kvt
import io.jadon.kvt.model.Entity
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Path(val path: String, val method: HttpMethod = HttpMethod.GET)

open class RestApi(private val version: Int) {

    lateinit var router: Router
    var initialized = false

    fun init(): Router {
        if (initialized) {
            throw RuntimeException("RestApi was already initialized!")
        }
        if (this.javaClass.name == RestApi::class.java.name) {
            throw RuntimeException("You need to implement RestApi with your JSON Paths!")
        }

        router = Router.router(Kvt.VERTX)
        initialized = true

        generatePaths()
//        val port = 2345
////        server.requestHandler({
////            println(it.path())
////            router.accept(it)
////        }).listen(port)
//        println("Server started @ http://localhost:$port")
        return router
    }

    private fun generatePaths() {
        // get paths through reflection
        this.javaClass.methods.filter { it.isAnnotationPresent(Path::class.java) }.forEach { method ->
            val annotation = method.getAnnotation(Path::class.java)
            val route = router.route(annotation.method, "/api/v$version" + annotation.path)

            route.handler { routingContext ->
                val parameters = mutableListOf<Any>()
                routingContext.request().params().forEachIndexed { i, routingParam ->
                    val methodParam = method.parameters.getOrNull(i)
                    if (methodParam == null) {
                        throw RuntimeException("Called ${method.name} with ${routingParam.key}=${routingParam.value}")
                    } else {
                        parameters.add(when (methodParam.type) {
                            Int::class.java -> routingParam.value.toInt()
                            else -> routingParam.value
                        })
                    }
                }
                if (parameters.size == method.parameterCount) {
                    val obj = method.invoke(this, *parameters.toTypedArray())
                    if (obj is JsonObject) {
                        routingContext.response().putHeader("content-type", "text/json")
                        // TODO: Prod - obj.encode()
                        routingContext.response().end(obj.encodePrettily())
                    } else {
                        throw RuntimeException("Method didn't return a JsonObject! ($obj)")
                    }
                } else {
                    throw RuntimeException("Parameter size is not correct! ($parameters)")
                }
            }
        }
    }
}

object RestApiV1 : RestApi(1) {

    private val errorJson: JsonObject = {
        val j = JsonObject()
        j.put("error", "null")
        j
    }()

    private fun encode(obj: Entity?): JsonObject = if (obj == null) errorJson else JsonObject(Json.encode(obj))
//    private fun encode(objs: List<Entity>): JsonArray = JsonArray(objs.map { Json.encode(it) })

    // song paths

    @Path("/song/:id")
    fun song(id: Int): JsonObject {
        return encode(Kvt.DB.getSong(id).get())
    }

    // artist paths

    @Path("/artist/:id")
    fun artist(id: Int): JsonObject {
        return encode(Kvt.DB.getArtist(id).get())
    }

    // album paths

    @Path("/album/:id")
    fun album(id: Int): JsonObject {
        return encode(Kvt.DB.getAlbum(id).get())
    }

    @Path("/search/:q")
    fun search(q: String): JsonObject {
        val o = JsonObject()
        o.put("search", q)
        o.put("artistIds", JsonArray(Kvt.DB.searchArtists(q).get()))
        o.put("songIds", JsonArray(Kvt.DB.searchSongs(q).get()))
        o.put("albumIds", JsonArray(Kvt.DB.searchAlbums(q).get()))
        return o
    }

}
