package io.em2m.search.core.parser

interface SchemaMapper {

    fun mapPath(field: String): String

    fun toString(field: String, value: Any): String?

    fun valueOf(field: String, str: String): Any?

}