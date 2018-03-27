package io.em2m.search.es

import feign.RequestTemplate
import feign.codec.Encoder
import java.lang.reflect.Type

class TextPlainEncoder(val delegate: Encoder) : Encoder {

    override fun encode(obj: Any, bodyType: Type, request: RequestTemplate) {
        val headers = request.headers()["Content-Type"]
        if (headers?.contains("text/plain") == true) {
            request.body(obj.toString())
        } else {
            delegate.encode(obj, bodyType, request)
        }
    }
}
