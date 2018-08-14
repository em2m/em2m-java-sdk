package io.em2m.actions.runtimes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.MultipartData
import io.em2m.actions.model.Problem
import io.em2m.flows.FlowNotFound
import io.em2m.flows.Processor
import java.io.InputStream
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class ServletRuntime(private val actionPrefix: String, private val processor: Processor<ActionContext>, val mapper: ObjectMapper = jacksonObjectMapper()) {

    fun process(actionName: String, request: HttpServletRequest, response: HttpServletResponse) {

        val env = createEnvironment(request)
        val contentType = request.contentType
        val parts = if (contentType.startsWith("multipart")) request.parts.toList() else emptyList()
        val multipart = MultipartData.fromParts(parts)
        val context = ActionContext("$actionPrefix:$actionName",
                inputStream = request.inputStream as InputStream,
                parts = parts,
                environment = env,
                multipart = multipart,
                response = ServletResponse(response))
        context.scope["servletContext"] = request
        try {
            processor.process(actionName, context).toBlocking().subscribe(
                    {

                    },
                    {
                        handleError(response, context, it)
                    }
            )
        } catch (error: FlowNotFound) {
            handleError(response, context, error)
        }
    }

    private fun handleError(response: HttpServletResponse, context: ActionContext, error: Throwable) {
        val problem = Problem.convert(error)
        if (context.debug) {
            problem.setAny("stackTrace", error.stackTrace)
        }
        response.contentType = "application/json"
        response.status = problem.status
        mapper.writeValue(response.outputStream, problem)
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
        val cookies = servletRequest.cookies

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
                "cookies" to cookies
        )
    }

}
