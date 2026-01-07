package io.em2m.search.core.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.em2m.utils.Fallback
import io.em2m.utils.MultiException
import io.em2m.utils.OperationType
import io.em2m.utils.getOrFallback
import java.io.Closeable
import java.util.*

fun interface Searchable<T> {
    fun search(request: SearchRequest): SearchResult<T>
}

interface SyncDao<T> : Searchable<T>, Closeable {

    fun create(entity: T): T?

    fun deleteById(id: String): Boolean

    fun exists(id: String): Boolean

    operator fun contains(id: String): Boolean = exists(id)

    override fun search(request: SearchRequest): SearchResult<T>

    fun count(query: Query): Long

    fun findById(id: String): T?

    operator fun get(id: String): T? = findById(id)

    fun findOne(query: Query): T?

    fun save(id: String, entity: T): T?

    operator fun set(id: String, value: T) = this.save(id, value)

    fun saveBatch(entities: List<T>): List<T>

    fun upsert(id: String, entity: T): T?

    fun upsertBatch(entities: List<T>): List<T>

    fun getPriority(type: OperationType): Int {
        return OperationType.MEDIUM_PRIORITY
    }

}

data class StreamRowsRequest(
    val fields: List<Field>,
    val query: Query = MatchAllQuery(),
    val sorts: List<DocSort> = emptyList(),
    val params: Map<String, Any> = emptyMap())

data class StreamItemsRequest(
    val query: Query = MatchAllQuery(),
    val sorts: List<DocSort> = emptyList(),
    val params: Map<String, Any> = emptyMap()
)

interface StreamableDao<T> : SyncDao<T> {
    fun streamRows(fields: List<Field>, query: Query = MatchAllQuery(), sorts: List<DocSort> = emptyList(), params: Map<String, Any> = emptyMap()) : Iterator<List<Any?>>
    fun streamItems(query: Query = MatchAllQuery(), sorts: List<DocSort> = emptyList(), params: Map<String, Any> = emptyMap()): Iterator<T>

    fun streamRows(request: StreamRowsRequest): Iterator<List<Any?>> = streamRows(
        fields = request.fields,
        query = request.query,
        sorts = request.sorts,
        params = request.params
    )

    fun streamItems(request: StreamItemsRequest): Iterator<T> = streamItems(
        query = request.query,
        sorts = request.sorts,
        params = request.params
    )

}

interface IdMapper<T> {
    val idField: String
    fun getId(obj: T): String
    fun setId(obj: T, id: String): T
    fun generateId(): String {
        return UUID.randomUUID().toString()
    }
    operator fun invoke(obj: T): String = this.getId(obj)
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

class FallbackSyncDao<T>(primary: SyncDao<T>,
                         fallbacks: List<SyncDao<T>> = emptyList())
    : SyncDao<T>, Fallback<SyncDao<T>>(primary, fallbacks) {

    @Throws(MultiException::class)
    override fun create(entity: T): T? {
        return getOrNull { it.create(entity) }
    }

    @Throws(MultiException::class)
    override fun deleteById(id: String): Boolean {
        return getOrThrow { it.deleteById(id) }
    }

    @Throws(MultiException::class)
    override fun exists(id: String): Boolean {
        return getOrThrow { it.exists(id) }
    }

    @Throws(MultiException::class)
    override fun search(request: SearchRequest): SearchResult<T> {
        return getOrThrow { it.search(request) }
    }

    @Throws(MultiException::class)
    override fun count(query: Query): Long {
        return getOrThrow { it.count(query) }
    }

    @Throws(MultiException::class)
    override fun findById(id: String): T? {
        return getOrThrow { it.findById(id) }
    }

    override fun findOne(query: Query): T? {
        return getOrFallback(primary, fallbacks) { it.findOne(query)!! }
            .getOrNull()
    }

    override fun save(id: String, entity: T): T? {
        return getOrFallback(primary, fallbacks) { it.save(id, entity)!! }
            .getOrNull()
    }

    @Throws(MultiException::class)
    override fun saveBatch(entities: List<T>): List<T> {
        return getOrFallback(primary, fallbacks) { it.saveBatch(entities) }
            .getOrThrow()
    }

    @Throws(MultiException::class)
    override fun close() {
        return getOrFallback(primary, fallbacks) { it.run { it.close() } }
            .getOrThrow()
    }

    override fun upsert(id: String, entity: T): T? {
        return getOrNull { it.upsert(id, entity) }
    }

    override fun upsertBatch(entities: List<T>): List<T> {
        return getOrElse(emptyList()) { it.upsertBatch(entities) }
    }

}

fun <T> StreamableDao<T>.streamTo(other: SyncDao<T>,
                                  chunkSize: Int = 16,
                                  query: Query = MatchAllQuery(),
                                  sorts: List<DocSort> = emptyList(),
                                  params: Map<String, Any> = emptyMap()) {
    streamContents(this, other,
        chunkSize, query, sorts, params)
}

private fun <T> streamContents(from: StreamableDao<T>, to: SyncDao<T>,
                               chunkSize: Int,
                               query: Query = MatchAllQuery(),
                               sorts: List<DocSort> = emptyList(),
                               params: Map<String, Any> = emptyMap()) {
    from.streamItems(query, sorts, params)
        .asSequence()
        .chunked(chunkSize)
        .forEach(to::saveBatch)
}
