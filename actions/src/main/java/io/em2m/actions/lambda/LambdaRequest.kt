package io.em2m.actions.lambda

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class LambdaRequest(
        val method: String? = null,
        val httpMethod: String? = null,
        val headers: Map<String, String>? = emptyMap(),
        val body: String? = null,
        val requestUri: String?,
        val path: String? = null,
        val pathParameters: Map<String, String>? = emptyMap(),
        val queryStringParameters: Map<String, String>? = emptyMap()
) {
    val contentType
        get() = headers?.get("Content-Type")
}
