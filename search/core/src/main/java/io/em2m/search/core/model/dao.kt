package io.em2m.search.core.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.Closeable
import java.util.*


interface SyncDao<T> : Closeable {

    fun create(entity: T): T?

    fun deleteById(id: String): Boolean

    fun exists(id: String): Boolean

    fun search(request: SearchRequest): SearchResult<T>

    fun count(query: Query): Long

    fun findById(id: String): T?

    fun findOne(query: Query): T?

    fun save(id: String, entity: T): T?

    fun saveBatch(entities: List<T>): List<T>

}

interface IdMapper<T> {
    val idField: String
    fun getId(obj: T): String
    fun setId(obj: T, id: String): T
    fun generateId(): String {
        return UUID.randomUUID().toString()
    }
}

class FnIdMapper<T>(override val idField: String, val getFn: ((T) -> String), val setFn: ((T, String) -> T)? = null) : IdMapper<T> {

    override fun getId(obj: T): String {
        return getFn(obj)
    }

    override fun setId(obj: T, id: String): T {
        return if (setFn != null) {
            setFn.invoke(obj, id)
        } else {
            throw NotImplementedError()
        }
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

