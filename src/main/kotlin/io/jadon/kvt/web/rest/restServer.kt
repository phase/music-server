package io.jadon.kvt.web.rest

import io.jadon.kvt.Kvt
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Path(val path: String, val method: HttpMethod)

open class RestApi(private val version: Int) {

    lateinit var server: HttpServer
    lateinit var router: Router
    var initialized = false

    fun init() {
        if (initialized) {
            throw RuntimeException("RestApi was already initialized!")
        }
        if (this.javaClass.name == RestApi::class.java.name) {
            throw RuntimeException("You need to implement RestApi with your JSON Paths!")
        }

        server = Kvt.VERTX.createHttpServer()
        router = Router.router(Kvt.VERTX)
        initialized = true

        generatePaths()
        server.requestHandler({ router.accept(it) }).listen(8080)
        println("Server started")
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
                        routingContext.response().end(obj.encode())
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
    @Path("/song/:id", HttpMethod.GET)
    fun song(id: Int): JsonObject {
        val j = JsonObject()
        j.put("id", id)
        return j
    }
}
