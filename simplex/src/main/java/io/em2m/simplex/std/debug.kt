package io.em2m.simplex.std

import io.em2m.simplex.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

class LogPipe : PipeTransform {

    private var level = "info"
    private var log: Logger = LoggerFactory.getLogger(javaClass)

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            level = args.first()
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        when (level) {
            "debug" -> log.debug(value.toString())
            "warn" -> log.warn(value.toString())
            "error" -> log.error(value.toString())
            else -> log.info(value.toString())
        }
        return value
    }

}

object Debug {
    val execs = BasicExecResolver()
        .handler("log") { LogHandler() }
    val pipes = BasicPipeTransformResolver()
        .transform("log") { LogPipe() }
}

