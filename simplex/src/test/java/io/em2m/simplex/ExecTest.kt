package io.em2m.simplex

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.simplex.model.*
import org.junit.Test
import org.slf4j.LoggerFactory


class ExecTest {

    val simplex = Simplex()
    val parser = simplex.parser
    val mapper = jacksonObjectMapper()

    init {
        simplex.execs(BasicExecResolver().handler("log") { LogHandler() })
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
                    }""".trimIndent())
        val context = HashMap<String, Any?>()
        simplex.exec(exec, context)
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

        override fun call(context: ExprContext, params: Map<String, Any?>) {
            val value = params["value"]?.toString()
            if (value != null) {
                when (level) {
                    "debug" -> log.debug(value)
                    "warn" -> log.warn(value)
                    "error" -> log.error(value)
                    else -> log.info(value.toString())
                }
            }
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

        override fun call(context: ExprContext, params: Map<String, Any?>) {
        }

    }

}