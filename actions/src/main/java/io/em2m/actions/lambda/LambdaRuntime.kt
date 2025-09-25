package io.em2m.actions.lambda

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionProcessor
import io.em2m.actions.model.MultipartData
import io.em2m.problem.Problem
import io.em2m.policy.model.Claims
import io.em2m.utils.parseCharset
import java.util.*

open class LambdaRuntime(
        private val actionPrefix: String,
        private val processor: ActionProcessor,
        private val mapper: ObjectMapper = jacksonObjectMapper()) {

    fun process(actionName: String, request: LambdaRequest): LambdaResponse {

        val response = LambdaResponse()
        val env = createEnvironment(request)

        // TODO: Detect and pars MultiPart
        val multipart: MultipartData? = null

        val contentType = request.contentType
        val charset = parseCharset(contentType)

        val context = ActionContext(
                actionName = "$actionPrefix:$actionName",
                claims = Claims(),
                inputStream = request.body?.toByteArray(charset)?.inputStream() ?: byteArrayOf().inputStream(),
                environment = env.toMutableMap(),
                multipart = multipart,
                response = response)
        context.scope["servletContext"] = request
        try {
            processor.process(context)
        } catch (error: Exception) {
            handleError(context, error)
        }
        return response
    }

    protected open fun mapError(error: Throwable): Problem {
        return Problem.convert(error)
    }

    private fun handleError(context: ActionContext, error: Throwable) {
        val problem = mapError(error)
        context.error = error
        if (context.debug) {
            problem.setAny("stackTrace", error.stackTrace)
        }
        context.response.entity = problem
        context.response.statusCode = problem.status
        context.response.contentType = "application/json"
        processor.handleError(context)
    }

    private fun createEnvironment(request: LambdaRequest): Map<String, Any?> {
        val headers = request.headers ?: emptyMap()
        val currentTime = Date()
        val sourceIp = headers["X-Forwarded-For"]
        val referer = headers["referer"]
        val token = headers["Authorization"]?.replace("Bearer ", "")
        val userAgent = headers["User-Agent"]
        val secureTransport = true
        val contentType = request.contentType
        val contentEncoding = headers["Content-Encoding"]?.lowercase(Locale.getDefault())

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
                "Method" to (request.httpMethod ?: request.method),
                "Parameters" to request.queryStringParameters
        )
    }

}
