package io.em2m.actions

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.name.Names
import io.em2m.actions.model.*
import io.em2m.actions.model.Priorities.Companion.MAIN
import io.em2m.actions.servlet.ActionServlet
import io.em2m.actions.servlet.ServletRuntime
import io.em2m.actions.xforms.JacksonRequestTransformer
import io.em2m.actions.xforms.JacksonResponseTransformer
import io.em2m.actions.xforms.LoggingTransformer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.slf4j.LoggerFactory
import jakarta.servlet.MultipartConfigElement


class ExampleServer {

    private val port = 9191

    private val server = Server(port)

    init {
        val context = ServletContextHandler(ServletContextHandler.SESSIONS)
        context.contextPath = "/"
        // context.addServlet(ServletHolder(SampleSseServlet()), "/sse")
        val holder = ServletHolder(TestActionServlet())
        holder.registration.setMultipartConfig(
            MultipartConfigElement(
                "/tmp/uploads",
                1024L * 1024L * 50L,
                1024L * 1024L * 50L,
                (1024 * 1024).toInt()
            )
        )
        context.addServlet(holder, "/demo/actions/*")

        val handlers = HandlerList(context)
        server.handler = handlers
    }

    fun start() {
        server.start()
        println("Server listening on port $port")
        server.stopAtShutdown = true
    }


    class TestModule : Module {

        override fun configure(binder: Binder) {
            binder.bindConstant().annotatedWith(Names.named("JAVA_ENV")).to("dev")
        }
    }

    class Log : ActionFlow {
        override val transformers = listOf(
            LoggingTransformer(LoggerFactory.getLogger(javaClass), { it.toString() }, MAIN)
        )
    }

    class Echo : TypedActionFlow<Any, Any>(Any::class.java, Any::class.java) {
        override fun main(context: ActionContext, req: Any): Any? {
            return context.request
        }
    }

    @TransformerOptIn(AuditRequestTransformer::class)
    class Trace : TypedActionFlow<Any, Any>(Any::class.java, Any::class.java) {
        override fun main(context: ActionContext, req: Any): Any? {
            return context.request
        }
    }

    class AuditRequestTransformer: ActionTransformer {
        override val priority = Priorities.AUDIT

        override fun doOnNext(ctx: ActionContext) {
            println(ctx.requestId)
        }
    }

    data class UploadRequest(val foo: String, val hasHeaders: Boolean)

    class Upload : TypedActionFlow<Any, Any>(UploadRequest::class.java, Any::class.java) {
        override fun main(context: ActionContext) {
            val filePart = context.multipart?.files?.get("file")
            println("FilePart: $filePart")
        }
    }

    class TestActionServlet : ActionServlet() {

        private val processor = ActionProcessorBuilder()
            .prefix("demo")
            .flow(Log::class)
            .flow(Echo::class)
            .flow(Upload::class)
            .flow(Trace::class)
            .flow("demo:stream", StreamingActionFlow::class)
            .module(TestModule())
            .transformer(JacksonRequestTransformer())
            .transformer(JacksonResponseTransformer())
            .transformer(OptionalActionTransformer(AuditRequestTransformer()))
            .build()

        override val runtime = ServletRuntime("demo", processor)

    }

}

fun main() {
    ExampleServer().start()
}
