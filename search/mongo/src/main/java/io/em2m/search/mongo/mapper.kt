package io.em2m.search.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.scaleset.geo.geojson.GeoJsonModule
import org.bson.Document

interface DocumentMapper<T> {
    fun toDocument(item: T): Document
    fun fromDocument(document: Document): T
}

class JacksonDocumentMapper<T>(val tClass: Class<T>, val idField: String = "_id", val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())) : DocumentMapper<T> {

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