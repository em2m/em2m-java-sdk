package io.em2m.actions.lambda

import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.actions.model.Response
import java.io.ByteArrayOutputStream
import java.io.OutputStream


class LambdaResponse : Response {

    override var statusCode: Int = 200

    override var entity: Any? = null

    override var contentType: String?
        get() = headers.get("Content-Type")
        set(value) = headers.set("Content-Type", value)

    override val headers = LambdaResponseHeaders()

    override val outputStream: OutputStream by lazy<OutputStream> {
        ByteArrayOutputStream()
    }

    class LambdaResponseHeaders() : Response.Headers {

        val data: MutableMap<String, String?> = HashMap()

        override fun set(key: String, value: String?) {
            data[key] = value
        }

        override fun get(key: String): String? {
            return data[key]
        }
    }

    fun toData(mapper: ObjectMapper): ResponseData {
        // TODO: Check OutputStream
        val body = mapper.writeValueAsString(entity)
        return ResponseData(false, statusCode, headers.data, body)
    }

    data class ResponseData(
            val isBase64Encoded: Boolean = false,
            val statusCode: Int,
            val headers: Map<String, String?>,
            val body: String
    )

}