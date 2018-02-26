package io.em2m.search.core.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.simplex.parser.DateMathParser
import java.util.*

class SimpleSchemaMapper(private val defaultField: String, val objectMapper: ObjectMapper = jacksonObjectMapper()) : SchemaMapper {

    private val aliases = HashMap<String, String>()

    private val types = HashMap<String, Class<*>>()

    override fun typeOf(field: String): Class<out Any>? {
        val f = aliases.getOrDefault(field, field)
        return types.get(f)
    }

    override fun mapPath(path: String): String {
        var result: String? = aliases[path]
        if (result == null || result.isEmpty()) {
            result = path
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

    fun <T> convertValue(from: Any?, typeRef: Class<T>): T {
        return objectMapper.convertValue(from, typeRef)
    }

    fun withMapping(field: String, type: Class<*>): SimpleSchemaMapper {
        types.put(field, type)
        return this
    }

    fun withAlias(alias: String, value: String): SimpleSchemaMapper {
        aliases.put(alias, value)
        return this
    }

    companion object {
        private val dateParser = DateMathParser()
    }
}