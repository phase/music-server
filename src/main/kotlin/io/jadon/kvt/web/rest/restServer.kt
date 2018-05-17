package io.jadon.kvt.web.rest

import io.jadon.kvt.Kvt
import io.jadon.kvt.model.Entity
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
                } else {
                    throw RuntimeException("Method didn't return a JsonObject! ($obj)")
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

    // account

    @Path("/login", HttpMethod.POST)
    fun login(username: String, password: String): JsonObject {
        val user = Kvt.DB.getUserFromName(username).get()
        val correctPassword = BCrypt.checkpw(password, user.passwordHash)
        return if (correctPassword) {
            val token = Kvt.DB.loginUser(user).get()
            val o = JsonObject()
            o.put("token", token.toString())
            o
        } else {
            errorJson("wrong password")
        }
    }

}
