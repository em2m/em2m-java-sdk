package io.em2m.search.core.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

class SimpleSchemaMapper(private val defaultField: String, val objectMapper: ObjectMapper = jacksonObjectMapper()) : SchemaMapper {

    private val aliases = HashMap<String, String>()

    private val types = HashMap<String, Class<*>>()

    override fun typeOf(field: String): Class<out Any>? {
        val f = aliases.getOrDefault(field, field)
        return types[f]
    }

    override fun mapPath(field: String): String {
        var result: String? = aliases[field]
        if (result == null || result.isEmpty()) {
            result = field
        }
        return result
    }

    override fun toString(field: String, value: Any): String? {
        return convertValue(value, String::class.java)
    }

    override fun valueOf(field: String, str: String): Any? {
        var type: Class<*>? = types[field]
        if (type == null) {
            type = String::class.java
        }
        return convertValue(str, type)
    }

    private fun <T> convertValue(from: Any?, typeRef: Class<T>): T {
        return objectMapper.convertValue(from, typeRef)
    }

    fun withMapping(field: String, type: Class<*>): SimpleSchemaMapper {
        types[field] = type
        return this
    }

    fun withAlias(alias: String, value: String): SimpleSchemaMapper {
        aliases[alias] = value
        return this
    }

}