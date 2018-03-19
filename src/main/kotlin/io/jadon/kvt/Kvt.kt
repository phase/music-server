package io.jadon.kvt

import io.jadon.kvt.db.Database
import io.vertx.core.Vertx

object Kvt {

    lateinit var database: Database
    lateinit var vertx: Vertx

    @JvmStatic
    fun main(args: Array<String>) {
        vertx = Vertx.vertx()

    }

}
