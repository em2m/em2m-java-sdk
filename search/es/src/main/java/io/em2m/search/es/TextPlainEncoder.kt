package io.em2m.search.es

import feign.RequestTemplate
import feign.codec.Encoder
import java.lang.reflect.Type

class TextPlainEncoder(private val delegate: Encoder) : Encoder {

    override fun encode(obj: Any, bodyType: Type, request: RequestTemplate) {
        val headers = request.headers()["Content-Type"]
        if (headers?.contains("text/plain") == true || headers?.contains("application/x-ndjson") == true) {
            request.body(obj.toString())
        } else {
            delegate.encode(obj, bodyType, request)
        }
    }
}
