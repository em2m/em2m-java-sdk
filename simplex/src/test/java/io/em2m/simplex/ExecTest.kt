package io.em2m.simplex

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.simplex.model.*
import io.em2m.simplex.parser.SimplexModule
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.assertNotNull


class ExecTest {

    private val simplex = Simplex()

    init {
        simplex.execs(BasicExecResolver()
                .handler("log") { LogHandler() }
                .handler("http") { HttpHandler() }
        )
    }

    private val mapper = jacksonObjectMapper().registerModule(SimplexModule(simplex))

    @Test
    fun testLog() {

        val exec: Expr = mapper.readValue(
                """
                    {
                     "@exec": "log",
                     "value": "value"
                    }
                    """)
        val context = HashMap<String, Any?>()
        exec.call(context)
    }

    @Test
    fun testHttp() {
        val exec: Expr = mapper.readValue(
                """
                 {
                   "@exec": "http",
                   "url": "https://jsonplaceholder.typicode.com/posts/1"
                 }   
                """)
        val result = exec.call(emptyMap())
        assertNotNull(result)
    }

    class LogHandler : ExecHandler {

        private var log: Logger = LoggerFactory.getLogger(javaClass)
        private var defaultLevel = "info"

        override fun call(context: ExprContext, op: String, params: Map<String, Any?>): Any? {
            val level = params["level"]?.toString() ?: defaultLevel
            val value = params["value"]?.toString()
            return if (value != null) {
                when (level) {
                    "debug" -> log.debug(value)
                    "warn" -> log.warn(value)
                    "error" -> log.error(value)
                    else -> log.info(value.toString())
                }
            } else null
        }

    }

    class HttpHandler : ExecHandler {

        override fun call(context: ExprContext, op: String, params: Map<String, Any?>): Any? {
            val url = URL(params["url"].toString())
            return with(url.openConnection() as HttpURLConnection) {
                println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
                inputStream.bufferedReader().readText()
            }
        }

    }

}