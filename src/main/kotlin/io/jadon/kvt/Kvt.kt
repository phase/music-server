package io.jadon.kvt

import io.jadon.kvt.db.Database
import io.jadon.kvt.db.DummyDatabase
import io.jadon.kvt.web.rest.RestApiV1
import io.vertx.core.Vertx

object Kvt {

    lateinit var DB: Database
    lateinit var VERTX: Vertx

    @JvmStatic
    fun main(args: Array<String>) {
        DB = DummyDatabase()
        VERTX = Vertx.vertx()
        RestApiV1.init()
    }

}
