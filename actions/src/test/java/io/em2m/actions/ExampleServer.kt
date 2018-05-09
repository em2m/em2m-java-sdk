package io.em2m.actions

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.name.Names
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.TypedActionFlow
import io.em2m.actions.runtimes.ServletRuntime
import io.em2m.actions.xforms.JacksonRequestTransformer
import io.em2m.actions.xforms.JacksonResponseTransformer
import io.em2m.actions.xforms.LoggingTransformer
import io.em2m.flows.BasicProcessor
import io.em2m.flows.Flow
import io.em2m.flows.Priorities.Companion.MAIN
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.slf4j.LoggerFactory
import rx.Observable
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

    class LoggingFlow : Flow<ActionContext> {
        override val transformers = listOf(
                LoggingTransformer(LoggerFactory.getLogger(javaClass), { it.toString() }, MAIN)
        )
    }

    class EchoFlow : TypedActionFlow<Any, Any>(Any::class.java, Any::class.java) {
        override fun main(obs: Observable<ActionContext>): Observable<ActionContext> {
            return obs.doOnNext {
                response(it, it.request)
            }
        }
    }

    data class UploadRequest(val foo: String, val hasHeaders: Boolean)

    class UploadFlow : TypedActionFlow<Any, Any>(UploadRequest::class.java, Any::class.java) {
        override fun main(obs: Observable<ActionContext>): Observable<ActionContext> {
            return obs.doOnNext { context ->
                val filePart = context.parts.find { it.name == "file" }
                response(context, context.request)
            }
        }
    }

    class ActionServlet : HttpServlet() {

        private val processor = BasicProcessor.Builder<ActionContext>()
                .module(TestModule())
                .transformer(JacksonRequestTransformer())
                .transformer(JacksonResponseTransformer())
                .flow("Log", LoggingFlow::class)
                .flow("Echo", EchoFlow::class)
                .flow("Upload", UploadFlow::class)
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