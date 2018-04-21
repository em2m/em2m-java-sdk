package io.em2m.actions.model

import java.io.OutputStream

interface Response {
    var statusCode: Int
    var contentType: String?
    var entity: Any?
    val headers: Headers
    val outputStream: OutputStream

    interface Headers {
        fun set(key: String, value: String?)
        fun get(key: String): String?
        fun putAll(other: Map<String, String?>) {
            other.forEach { set(it.key, it.value) }
        }
    }
}