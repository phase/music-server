package io.jadon.kvt.web.rest

import io.jadon.kvt.Kvt
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Path(val path: String, val method: HttpMethod)

object RestApi {

    lateinit var server: HttpServer
    lateinit var router: Router
    var initialized = false

    fun init() {
        if (initialized) {
            throw RuntimeException("RestApi was already initialized!")
        }
        server = Kvt.vertx.createHttpServer()
        router = Router.router(Kvt.vertx)
        initialized = true

        generatePaths()
    }

    private fun generatePaths() {
        // get paths through reflection
        this.javaClass.methods.filter { it.isAnnotationPresent(Path::class.java) }.forEach { method ->
            val annotation = method.getAnnotation(Path::class.java)
            val route = router.route(annotation.method, annotation.path)
            val parameters = mutableListOf<>()

            route.handler { routingContext ->
                routingContext.request().params().forEach { routingParam ->
                    val methodParam = method.parameters.find { it.name == routingParam.key }
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
                    val obj = method.invoke(this, parameters)
                    if (obj is JsonObject) {
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

    @Path("/song/:id", HttpMethod.GET)
    private fun song(id: Int): JsonObject {
        return JsonObject()
    }

}