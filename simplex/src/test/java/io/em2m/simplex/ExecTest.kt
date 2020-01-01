package io.em2m.simplex

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.simplex.model.*
import org.junit.Test
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL


class ExecTest {

    val simplex = Simplex()
    val parser = simplex.parser
    private val mapper = jacksonObjectMapper()

    init {
        simplex.execs(BasicExecResolver()
                .handler("log") { LogHandler() }
                .handler("http") { HttpHandler() }
        )
    }

    fun parse(expr: String): Expr {
        return parser.parse(expr)
    }

    @Test
    fun testLog() {

        val exec: Exec = mapper.readValue(
                """
                    {
                     "op": "log",
                     "config": { "level" : "info" },
                      "params": { "value": "value" }
                    }
                    """)
        val context = HashMap<String, Any?>()
        simplex.exec(exec, context)
    }

    @Test
    fun testHttp() {
        val exec: Exec = mapper.readValue(
                """
                 {
                     "op": "http",
                     "params": { 
                       "method":  "POST",
                       "url": "https://jsonplaceholder.typicode.com/posts/1"
                     }
                 }   
                """)
        val result = simplex.exec(exec, emptyMap())
    }

    class LogHandler : ExecHandler {

        var log = LoggerFactory.getLogger(javaClass)
        var level = "info"

        override fun configure(config: Map<String, Any?>) {
            val configLevel = (config["level"] as? String)?.toLowerCase()
            if (configLevel != null) {
                level = configLevel
            }
        }

        override fun call(context: ExprContext, op: String, params: Map<String, Any?>): ExecResult {
            val value = params["value"]?.toString()
            if (value != null) {
                when (level) {
                    "debug" -> log.debug(value)
                    "warn" -> log.warn(value)
                    "error" -> log.error(value)
                    else -> log.info(value.toString())
                }
            }
            return ExecResult()
        }

    }

    class HttpHandler : ExecHandler {

        var method = "POST"

        override fun configure(config: Map<String, Any?>) {
            (config["method"] as? String)?.also {
                method = it
            }
            // URL
            // request / body
            // headers
        }

        override fun call(context: ExprContext, op: String, params: Map<String, Any?>): ExecResult {
            val url = URL(params["url"].toString())
            var value: Any? = null
            with(url.openConnection() as HttpURLConnection) {
                println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
                value = inputStream.bufferedReader().readText()
            }
            return ExecResult(value)
        }

    }

}