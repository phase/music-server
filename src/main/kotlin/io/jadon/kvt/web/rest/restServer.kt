package io.jadon.kvt.web.rest

import io.jadon.kvt.Kvt
import io.jadon.kvt.model.AlbumId
import io.jadon.kvt.model.ArtistId
import io.jadon.kvt.model.Entity
import io.jadon.kvt.model.SongId
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.MultiMap
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import org.mindrot.jbcrypt.BCrypt

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Path(val path: String, val method: HttpMethod = HttpMethod.GET)

open class RestApi(private val version: Int) {

    protected fun errorJson(error: String): JsonObject {
        val j = JsonObject()
        j.put("error", error)
        return j
    }

    lateinit var router: Router
    var initialized = false

    fun init(router: Router) {
        if (initialized) {
            throw RuntimeException("RestApi was already initialized!")
        }
        if (this.javaClass.name == RestApi::class.java.name) {
            throw RuntimeException("You need to implement RestApi with your JSON Paths!")
        }
        this.router = router

        initialized = true

        generatePaths()
    }

    private fun generatePaths() {
        // get paths through reflection
        this.javaClass.methods.filter { it.isAnnotationPresent(Path::class.java) }.forEach { method ->
            val annotation = method.getAnnotation(Path::class.java)
            val route = router.route(annotation.method, "/api/v$version" + annotation.path)

            route.handler { routingContext ->
                val parameters = mutableListOf<Any>()
                val paramMap: MultiMap? = when (annotation.method) {
                    HttpMethod.GET -> routingContext.request().params()
                    HttpMethod.POST -> routingContext.request().formAttributes()
                    else -> null
                }
                paramMap?.forEachIndexed { i, routingParam ->
                    val methodParam = method.parameters.getOrNull(i)
                    if (methodParam == null) {
                        throw RuntimeException("Called ${method.name} with ${routingParam.key}=${routingParam.value}")
                    } else {
                        parameters.add(when (methodParam.type) {
                            Int::class.java -> routingParam.value.toInt()
                            String::class.java -> routingParam.value.toString()
                            else -> routingParam.value
                        })
                    }
                }
                val obj = try {
                    method.invoke(this, *parameters.toTypedArray())
                } catch (e: Exception) {
                    errorJson("invalid call to ${method.name}")
                }
                if (obj is JsonObject) {
                    routingContext.response().putHeader("content-type", "text/json")
                    // TODO: Prod - obj.encode()
                    routingContext.response().end(obj.encodePrettily())
                } else if (obj is Future<*>) {
                    obj.setHandler {
                        routingContext.response().putHeader("content-type", "text/json")
                        if (it.succeeded()) {
                            val result = it.result()
                            if (result is JsonObject) {
                                routingContext.response().end(result.encodePrettily())
                            } else {
                                routingContext.response().end(
                                        errorJson("internal server error (future didn't return json object").encodePrettily()
                                )
                            }
                        } else {
                            // TODO: Prod - obj.encode()
                            routingContext.response().end(
                                    errorJson("internal server error (future didn't succeed").encodePrettily()
                            )
                        }
                    }
                } else {
                    throw RuntimeException("Method didn't return a JsonObject or Future! ($obj)")
                }
            }
        }
    }
}

object RestApiV1 : RestApi(1) {

    private fun encode(obj: Entity?, error: String = "null result"): JsonObject =
            if (obj == null) errorJson(error)
            else JsonObject(Json.encode(obj))

    // song paths

    @Path("/song/:id")
    fun song(id: Int): Future<JsonObject> {
        return Kvt.DB.getSong(id).compose {
            Future.succeededFuture(encode(it))
        }
    }

    // artist paths

    @Path("/artist/:id")
    fun artist(id: Int): Future<JsonObject> {
        return Kvt.DB.getArtist(id).compose {
            Future.succeededFuture(encode(it))
        }
    }

    // album paths

    @Path("/album/:id")
    fun album(id: Int): Future<JsonObject> {
        return Kvt.DB.getAlbum(id).compose {
            Future.succeededFuture(encode(it))
        }
    }

    @Path("/search/:q")
    fun search(q: String): Future<JsonObject> {
        return CompositeFuture.all(Kvt.DB.searchArtists(q), Kvt.DB.searchSongs(q), Kvt.DB.searchAlbums(q)).compose {
            val o = JsonObject()
            o.put("artistIds", JsonArray(it.resultAt<List<ArtistId>>(0)))
            o.put("songIds", JsonArray(it.resultAt<List<SongId>>(1)))
            o.put("albumIds", JsonArray(it.resultAt<List<AlbumId>>(2)))
            Future.succeededFuture(o)
        }
    }

    // account

    @Path("/login", HttpMethod.POST)
    fun login(username: String, password: String): Future<JsonObject> {
//        val user = Kvt.DB.getUserFromName(username).get()
//        val correctPassword = BCrypt.checkpw(password, user.passwordHash)
//        return if (correctPassword) {
//            val token = Kvt.DB.loginUser(user).get()
//            val o = JsonObject()
//            o.put("token", token.toString())
//            o
//        } else {
//            errorJson("wrong password")
//        }
        return Future.succeededFuture(JsonObject(mapOf("token" to "TODO")))
    }

    @Path("/validate")
    fun validate(token: String): Future<JsonObject> {
        return Kvt.DB.isValidToken(token).compose {
            val o = JsonObject()
            o.put("valid", it)
            Future.succeededFuture(o)
        }
    }

}
