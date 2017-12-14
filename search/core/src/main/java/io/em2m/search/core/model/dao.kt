package io.em2m.search.core.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import rx.Observable
import java.io.Closeable
import java.util.*

interface SearchDao<T> : Closeable {

    fun create(entity: T): Observable<T>

    fun deleteById(id: String): Observable<Boolean>

    fun exists(id: String): Observable<Boolean>

    fun search(request: SearchRequest): Observable<SearchResult<T>>

    fun count(query: Query): Observable<Long>

    fun findById(id: String): Observable<T?>

    fun findOne(query: Query): Observable<T?>

    fun save(id: String, entity: T): Observable<T>

    fun saveBatch(entities: List<T>): Observable<List<T>>

}

interface IdMapper<T> {
    val idField: String
    fun getId(obj: T): String
    fun setId(obj: T, id: String): T
    fun generateId(): String {
        return UUID.randomUUID().toString()
    }
}

interface DocMapper<T> {
    fun toDoc(obj: T): ObjectNode?
    fun fromDoc(doc: ObjectNode): T?
    fun toObject(value: JsonNode): Any?
}

open class JacksonDocMapper<T>(val tClass: Class<T>, val objectMapper: ObjectMapper, val debug: Boolean = false) : DocMapper<T> {

    override fun toDoc(obj: T): ObjectNode? {
        return try {
            tweakAfterMapping(objectMapper.convertValue(obj, ObjectNode::class.java))
        } catch (e: Exception) {
            if (debug) {
                e.printStackTrace()
            }
            null
        }
    }

    override fun fromDoc(doc: ObjectNode): T? {
        return try {
            objectMapper.convertValue(tweakBeforeMapping(doc), tClass)
        } catch (e: Exception) {
            if (debug) {
                e.printStackTrace()
            }
            null
        }
    }

    override fun toObject(value: JsonNode): Any? {
        return try {
            objectMapper.convertValue(value, Any::class.java)
        } catch (e: Exception) {
            if (debug) {
                e.printStackTrace()
            }
            null
        }
    }

    open fun tweakBeforeMapping(doc: ObjectNode): ObjectNode {
        return doc
    }

    open fun tweakAfterMapping(doc: ObjectNode): ObjectNode {
        return doc
    }
}

