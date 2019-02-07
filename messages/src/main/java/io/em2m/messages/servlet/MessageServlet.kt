package io.em2m.messages.servlet

import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.flows.FlowNotFound
import io.em2m.messages.model.MessageContext
import io.em2m.messages.model.MessageProcessor
import java.io.InputStream
import java.util.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


abstract class MessageServlet() : HttpServlet() {

    abstract val processor: MessageProcessor
    abstract val mapper: ObjectMapper

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        process(req, resp)
    }

    fun process(request: HttpServletRequest, response: HttpServletResponse) {
        val env = createEnvironment(request)
        val contentType: String? = request.contentType
        val context = MessageContext(
                inputStream = request.inputStream as InputStream,
                environment = env.toMutableMap())
        context.scope["servletContext"] = request
        try {
            processor.process(context).toBlocking().subscribe(
                    {

                    },
                    { error ->
                        handleError(response, context, error)
                    }
            )
        } catch (error: FlowNotFound) {
            handleError(response, context, error)
        }
    }

    private fun handleError(response: HttpServletResponse, context: MessageContext, error: Throwable) {
        processor.handleError(context).subscribe()
        response.contentType = "application/json"
        response.status = 500
        mapper.writeValue(response.outputStream, mapOf("message" to error.message))
    }

    private fun createEnvironment(servletRequest: HttpServletRequest): Map<String, Any?> {
        val currentTime = Date()
        val sourceIp = servletRequest.remoteAddr
        val referer = servletRequest.getHeader("referer")
        val token = servletRequest.getHeader("Authorization")?.replace("Bearer ", "")
        val userAgent = servletRequest.getHeader("user-agent")
        val secureTransport = servletRequest.isSecure
        val contentType = servletRequest.contentType
        val contentEncoding = servletRequest.getHeader("Content-Encoding")?.toLowerCase()
        val headers = servletRequest.headerNames.toList().associate { it to servletRequest.getHeaders(it).toList() }
        val cookies = servletRequest.cookies?.toList() ?: emptyList()

        return mapOf(
                "CurrentTime" to currentTime,
                "EpochTime" to currentTime.time,
                "SourceIp" to sourceIp,
                "Referer" to referer,
                "Token" to token,
                "UserAgent" to userAgent,
                "SecureTransport" to secureTransport,
                "ContentType" to contentType,
                "ContentEncoding" to contentEncoding,
                "Headers" to headers,
                "Method" to servletRequest.method,
                "cookies" to cookies,
                "Parameters" to servletRequest.parameterMap
        )
    }

}
