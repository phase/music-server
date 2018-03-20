package io.jadon.kvt

import io.jadon.kvt.db.Database
import io.jadon.kvt.web.rest.RestApiV1
import io.vertx.core.Vertx

object Kvt {

    lateinit var database: Database
    lateinit var vertx: Vertx

    @JvmStatic
    fun main(args: Array<String>) {
        vertx = Vertx.vertx()
        RestApiV1.init()
    }

}
