package io.em2m.search.core.parser

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.scaleset.utils.Coerce
import java.util.*

class SimpleSchemaMapper(private val defaultField: String) : SchemaMapper {
    private val aliases = HashMap<String, String>()
    private val coerce = Coerce(jacksonObjectMapper())

    private val simpleSchema = HashMap<String, Class<*>>()

    override fun mapPath(path: String): String {
        var result: String? = aliases[path]
        if (result == null || result.isEmpty()) {
            result = path
        }
        return result
    }

    override fun toString(field: String, value: Any): String? {
        return coerce.convert(value, String::class.java)
    }

    override fun valueOf(field: String, str: String): Any {
        var type: Class<*>? = simpleSchema[field]
        if (type == null) {
            type = String::class.java
        }
        return coerce.convert(str, type)
    }

    fun withMapping(field: String, type: Class<*>): SimpleSchemaMapper {
        simpleSchema.put(field, type)
        return this
    }

    fun withAlias(alias: String, value: String): SimpleSchemaMapper {
        aliases.put(alias, value)
        return this
    }
}