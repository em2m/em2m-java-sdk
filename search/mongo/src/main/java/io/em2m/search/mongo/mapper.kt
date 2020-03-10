package io.em2m.search.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.geo.geojson.GeoJsonModule
import org.bson.Document

interface DocumentMapper<T> {
    fun toDocument(item: T): Document
    fun fromDocument(document: Document): T
}

class JacksonDocumentMapper<T>(private val tClass: Class<T>, private val idField: String = "_id", private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())) : DocumentMapper<T> {

    override fun toDocument(item: T): Document {
        val result = objectMapper.convertValue(item, Document::class.java)
        if ("_id" != idField) {
            val id = result.remove(idField)
            result.put("_id", id)
        }
        return result
    }

    override fun fromDocument(document: Document): T {
        if ("_id" != idField) {
            val id = document.remove("_id")
            document.put(idField, id)
        }
        return objectMapper.convertValue(document, tClass)
    }

}