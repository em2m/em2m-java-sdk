package io.em2m.actions.lambda

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.model.Response
import java.io.ByteArrayOutputStream
import java.io.OutputStream


class LambdaResponse : Response {

    override var statusCode: Int = 200

    override var entity: Any? = null

    override var contentType: String?
        get() = headers.get("Content-Type")
        set(value) = headers.set("Content-Type", value)

    override val headers: Response.Headers = LambdaResponseHeaders()

    override val outputStream: OutputStream by lazy<OutputStream> {
        ByteArrayOutputStream()
    }

    inner class LambdaResponseHeaders() : Response.Headers {

        val headers = HashMap<String, String?>()

        override fun set(key: String, value: String?) {
            headers[key] = value
        }

        override fun get(key: String): String? {
            return headers[key]
        }
    }

    data class ResponseData(
            val isBase64Encoded: Boolean = false,
            val headers: Map<String, String>
    )

    companion object {

    }
}