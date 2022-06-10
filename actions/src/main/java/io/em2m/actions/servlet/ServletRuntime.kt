package io.em2m.actions.servlet

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionProcessor
import io.em2m.actions.model.MultipartData
import io.em2m.policy.model.Claims
import io.em2m.problem.Problem
import java.io.InputStream
import java.util.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Part


open class ServletRuntime(private val actionPrefix: String, private val processor: ActionProcessor, private val mapper: ObjectMapper = jacksonObjectMapper()) {

    fun process(actionName: String, request: HttpServletRequest, response: HttpServletResponse) {

        val env = createEnvironment(request)
        val contentType: String? = request.contentType
        val parts = if (contentType?.startsWith("multipart") == true) request.parts.toList() else emptyList()
        val multipart = fromParts(parts)
        val context = ActionContext(
                actionName = "$actionPrefix:$actionName",
                inputStream = request.inputStream as InputStream,
                environment = env.toMutableMap(),
                claims = Claims(),
                multipart = multipart,
                response = ServletResponse(response))
        context.scope["servletContext"] = request
        try {
            processor.process(context)
        } catch (error: Throwable) {
            handleError(response, context, error)
        }
    }

    protected open fun mapError(error: Throwable): Problem {
        return Problem.convert(error)
    }

    private fun handleError(response: HttpServletResponse, context: ActionContext, error: Throwable) {
        val problem = mapError(error)
        context.error = error
        if (context.debug) {
            problem.setAny("stackTrace", error.stackTrace)
        }
        context.response.entity = problem
        context.response.statusCode = problem.status
        context.response.contentType = "application/json"

        processor.handleError(context)

        response.contentType = "application/json"
        response.status = problem.status
        mapper.writeValue(response.outputStream, problem)
    }

    private fun createEnvironment(servletRequest: HttpServletRequest): Map<String, Any?> {
        val currentTime = Date()
        val sourceIp = servletRequest.remoteAddr
        val referer = servletRequest.getHeader("referer")
        val token = servletRequest.getHeader("Authorization")?.replace("Bearer ", "")
                // some special case rewrites to deal with bad clients
                .let { if (it == "undefined") null else it }
                .let { if (it == "null") null else it }
        val userAgent = servletRequest.getHeader("user-agent")
        val secureTransport = servletRequest.isSecure
        val contentType = servletRequest.contentType
        val contentEncoding = servletRequest.getHeader("Content-Encoding")?.lowercase(Locale.getDefault())
        val headers = servletRequest.headerNames.toList().associateWith { servletRequest.getHeaders(it).toList() }
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

    private fun fromParts(parts: List<Part>): MultipartData {

        val files = HashMap<String, MultipartData.File>()
        val form = HashMap<String, String>()
        parts.forEach { part ->
            val filename = extractFileName(part)
            if (filename?.isNotEmpty() == true) {
                val headers = HashMap<String, List<String>>()
                files[part.name] = MultipartData.File(filename, headers, part.inputStream)
            } else {
                try {
                    val name = part.name
                    val text = part.inputStream.reader().readText()
                    form[name] = text
                } catch (ex: Exception) {
                    MultipartData.log.error("Error parsing part ${part.name}", ex)
                }
            }
        }
        return MultipartData(files, form)
    }

    private fun extractFileName(part: Part): String? {
        val contentDisp = part.getHeader("content-disposition")
        val items = contentDisp.split(";")
        for (s in items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length - 1)
            }
        }
        return null
    }
}
