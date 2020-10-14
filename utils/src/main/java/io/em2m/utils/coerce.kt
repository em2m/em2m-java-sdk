package io.em2m.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass


object Coerce {
    val objectMapper = jacksonObjectMapper()
}

inline fun <reified T : Any> Any?.convertValue(valueType: KClass<T>, fallback: T? = null, objectMapper: ObjectMapper = Coerce.objectMapper): T? {
    return try {
        if (this != null) {
            objectMapper.convertValue(this, valueType.java)
        } else fallback
    } catch (t: Throwable) {
        fallback
    }
}

inline fun <reified T : Any> Any?.convertValue(valueType: Class<T>, fallback: T? = null, objectMapper: ObjectMapper = Coerce.objectMapper): T? {
    return try {
        if (this != null) {
            objectMapper.convertValue(this, valueType)
        } else fallback
    } catch (t: Throwable) {
        fallback
    }
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

fun Any?.isTruthy(): Boolean {
    return when (this) {
        is Boolean -> this
        is String -> this.isNotBlank()
        is Double -> this != 0 && this.isFinite()
        is Float -> (this != 0) && this.isFinite()
        is Number -> (this != 0)
        else -> this != null
    }
}

fun Any?.isFalsy(): Boolean {
    return !isTruthy()
}