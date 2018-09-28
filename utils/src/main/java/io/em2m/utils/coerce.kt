package io.em2m.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


object Coerce {
    val objectMapper = jacksonObjectMapper()
}

inline fun <reified T : Any> Any?.coerce(fallback: T? = null, objectMapper: ObjectMapper = Coerce.objectMapper): T? {
    return try {
        if (this != null) {
            objectMapper.convertValue(this)
        } else fallback
    } catch (t: Throwable) {
        fallback
    }
}

inline fun <reified T : Any> Any?.coerceNonNull(fallback: T? = null, objectMapper: ObjectMapper = Coerce.objectMapper): T {
    return try {
        if (this != null) {
            objectMapper.convertValue(this)
        } else requireNotNull(fallback)
    } catch (t: Throwable) {
        requireNotNull(fallback)
    }
}