package io.em2m.actions

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.name.Names
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionFlow
import io.em2m.actions.model.ActionProcessorBuilder
import io.em2m.actions.model.Priorities.Companion.MAIN
import io.em2m.actions.model.TypedActionFlow
import io.em2m.actions.servlet.ServletRuntime
import io.em2m.actions.xforms.JacksonRequestTransformer
import io.em2m.actions.xforms.JacksonResponseTransformer
import io.em2m.actions.xforms.LoggingTransformer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.slf4j.LoggerFactory
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class ExampleServer {

    private val port = 9191

    private val server = Server(port)

    init {
        val context = ServletContextHandler(ServletContextHandler.SESSIONS)
        context.contextPath = "/"
        // context.addServlet(ServletHolder(SampleSseServlet()), "/sse")
        val holder = ServletHolder(ActionServlet())
        holder.registration.setMultipartConfig(MultipartConfigElement("/tmp/uploads", 1024 * 1024 * 50, 1024 * 1024 * 50, (1024 * 1024).toInt()))
        context.addServlet(holder, "/actions/*")

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

    data class UploadRequest(val foo: String, val hasHeaders: Boolean)

    class Upload : TypedActionFlow<Any, Any>(UploadRequest::class.java, Any::class.java) {
        override fun main(context: ActionContext) {
            val filePart = context.multipart?.files?.get("file")
            println("FilePart: $filePart")
        }
    }

    class ActionServlet : HttpServlet() {

        private val processor = ActionProcessorBuilder()
                .prefix("demo")
                .flow(Log::class)
                .flow(ExampleServer.Echo::class)
                .flow(ExampleServer.Upload::class)
                .module(ExampleServer.TestModule())
                .transformer(JacksonRequestTransformer())
                .transformer(JacksonResponseTransformer())
                .build()

        private val runtime = ServletRuntime("demo", processor)

        override fun doPost(request: HttpServletRequest, response: HttpServletResponse) {
            val actionName = request.pathInfo.substring(1)
            runtime.process(actionName, request, response)
        }

    }

}

fun main(args: Array<String>) {
    ExampleServer().start()
}