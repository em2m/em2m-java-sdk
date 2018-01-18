package io.em2m.actions.runtimes

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionError
import io.em2m.flows.FlowNotFound
import io.em2m.flows.Processor
import java.io.InputStream
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class ServletRuntime(private val actionPrefix: String, private val processor: Processor<ActionContext>) {

    private val mapper = jacksonObjectMapper()

    fun process(actionName: String, request: HttpServletRequest, response: HttpServletResponse) {

        val env = createEnvironment(request)
        val parts = if ("multipart/form-data" == request.contentType) request.parts.toList() else emptyList()
        val context = ActionContext("$actionPrefix:$actionName", request.inputStream as InputStream, parts, environment = env)

        try {
            processor.process(actionName, context).toBlocking().subscribe(
                    {
                        response.contentType = "application/json"
                        response.status = HttpServletResponse.SC_OK
                        if (context.response.entity != null) {
                            mapper.writeValue(response.outputStream, context.response.entity)
                        }
                    },
                    { error ->
                        handleError(response, ActionError.convert(error, context))
                    }
            )
        } catch (ex: FlowNotFound) {
            handleError(response, ActionError.convert(ex, context))
        }
    }

    private fun handleError(response: HttpServletResponse, error: ActionError) {
        val entity = mapOf(
                "status" to error.status,
                "code" to error.code,
                "messages" to error.messages)
        response.contentType = "application/json"
        response.status = error.status
        mapper.writeValue(response.outputStream, entity)
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

        return mapOf(
                "CurrentTime" to currentTime,
                "EpochTime" to currentTime.time,
                "SourceIp" to sourceIp,
                "Referer" to referer,
                "Token" to token,
                "UserAgent" to userAgent,
                "SecureTransport" to secureTransport,
                "ContentType" to contentType,
                "ContentEncoding" to contentEncoding
        )
    }


}
