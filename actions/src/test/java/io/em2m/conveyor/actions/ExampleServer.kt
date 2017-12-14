package io.em2m.conveyor.actions

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.name.Names
import io.em2m.conveyor.actions.model.ActionContext
import io.em2m.conveyor.actions.runtimes.ServletRuntime
import io.em2m.conveyor.actions.xforms.JacksonRequestTransformer
import io.em2m.conveyor.actions.xforms.LoggingTransformer
import io.em2m.conveyor.flows.BasicProcessor
import io.em2m.conveyor.flows.Flow
import io.em2m.conveyor.flows.MainFlow
import io.em2m.conveyor.flows.Priorities.Companion.MAIN
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.slf4j.LoggerFactory
import rx.Observable
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class ExampleServer {

    private val port = 9191 // #850F88 - Dark Magenta

    private val server = Server(port)

    init {
        val context = ServletContextHandler(ServletContextHandler.SESSIONS)
        context.contextPath = "/"
        // context.addServlet(ServletHolder(SampleSseServlet()), "/sse")
        context.addServlet(ServletHolder(ActionServlet()), "/actions/*")

        val handlers = HandlerList(context)
        server.handler = handlers
    }

    fun start() {
        server.start()
        server.stopAtShutdown = true
    }

    fun shutdown() {
        server.stop()
    }

    class TestModule : Module {

        override fun configure(binder: Binder) {
            binder.bindConstant().annotatedWith(Names.named("JAVA_ENV")).to("dev")
        }
    }

    class ActionServlet : HttpServlet() {

        val loggingFlow = object : Flow<ActionContext> {
            override val transformers = listOf(
                    LoggingTransformer(LoggerFactory.getLogger(javaClass), { it.toString() }, MAIN)
            )
        }

        val echoFlow = object : MainFlow<ActionContext>(mutableListOf(JacksonRequestTransformer(Any::class.java))) {

            override fun call(source: Observable<ActionContext>): Observable<ActionContext> {
                return source.doOnNext {
                    it.response.entity = it.request
                }
            }
        }

        val processor = BasicProcessor.Builder<ActionContext>()
                .module(TestModule())
                .flow("Log", loggingFlow)
                .flow("Echo", echoFlow)
                .build()

        val runtime = ServletRuntime("openelt", processor)

        override fun doPost(request: HttpServletRequest, response: HttpServletResponse) {
            val actionName = request.pathInfo.substring(1)
            runtime.process(actionName, request, response)
        }

    }

}

fun main(args: Array<String>) {
    ExampleServer().start()
}