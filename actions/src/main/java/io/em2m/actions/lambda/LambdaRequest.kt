package io.em2m.actions.lambda


class LambdaRequest(
        val method: String,
        val headers: Map<String, String>,
        val body: String,
        val requestUri: String,
        val pathParameters: Map<String, String>,
        val queryStringParameters: Map<String, String>
) {
    val contentType
        get() = headers.get("Content-Type")
}
