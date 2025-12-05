package io.em2m.search.es8.dao

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.core.model.*
import io.em2m.search.es.EsSyncDaoUnionType
import io.em2m.search.es.RequestConverter
import io.em2m.search.es2.dao.Es2SyncDao
import io.em2m.search.es8.Es8Api
import java.util.*

class Es8SyncDao<T>(
    val es8Api: Es8Api,
    index: String,
    tClass: Class<T>,
    idMapper: IdMapper<T>,
    docMapper: DocMapper<T>? = null,
    objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
) : EsSyncDaoUnionType<T>(index, tClass, idMapper, docMapper, objectMapper) {

    override val docMapper: DocMapper<T> = docMapper ?: JacksonDocMapper(tClass, objectMapper)
    val requestConverter = RequestConverter(objectMapper)
    val resultConverter = Es8ResultConverter(this.docMapper)

    private fun String.urlEncode() = java.net.URLEncoder.encode(this, "UTF-8")

    override fun create(entity: T): T? {
        val id = generateId()
        val transformedEntity = idMapper.setId(entity, id)
        val doc = encode(transformedEntity)
        return if (doc != null) {
            es8Api.put(index, idMapper.getId(transformedEntity).urlEncode(), doc)
            entity
        } else null
    }

    override fun deleteById(id: String): Boolean {
        es8Api.delete(index, id.urlEncode())
        return true
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        val esReq = requestConverter.convert(request)
        val scroll = request.params["scroll"] as String?
        return if (scroll != null)
            resultConverter.convert(request, es8Api.search(index, scroll, esReq))
        else
            resultConverter.convert(request, es8Api.search(index, esReq))
    }

    fun scrollItems(request: SearchRequest, result: SearchResult<T>): Iterable<T> {
        // TODO - Consider creating a different request signature or validating search request is compatible
        val scroll = request.params["scroll"] as String
        return object : Iterable<T> {
            override fun iterator(): Iterator<T> {
                return ItemScrollIterator(request, result, scroll)
            }
        }
    }

    fun scrollRows(request: SearchRequest, result: SearchResult<T>): Iterable<Any?> {
        // TODO - Consider creating a different request signature or validating search request is compatible
        val scroll = request.params["scroll"] as String
        return object : Iterable<List<Any?>> {
            override fun iterator(): Iterator<List<Any?>> {
                return RowScrollIterator(request, result, scroll)
            }
        }
    }

    override fun save(id: String, entity: T): T? {
        idMapper.setId(entity, id)
        val doc = docMapper.toDoc(entity)
        return if (doc != null) {
            es8Api.put(index, idMapper.getId(entity).urlEncode(), doc)
            entity
        } else null
    }

    private fun bulkIndex(index: String, id: String, entity: T): String {
        val line1 = """{ "index": {"_index": "$index", "_id": "$id"} }"""
        val line2 = objectMapper.disable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(entity)
        return "$line1\n$line2\n"
    }

    override fun saveBatch(entities: List<T>): List<T> {
        val bulkRequest = entities.joinToString("") { bulkIndex(index, idMapper.getId(it), it) }
        val bulkResult = es8Api.bulkUpdate(bulkRequest)
        return if (bulkResult.errors) {
            val errorIds = bulkResult.items.filter {
                (it.index?.status ?: 0) >= 400
            }.map { it.index?._id }.toSet()
            entities.filter { errorIds.contains(idMapper.getId(it)) }
        } else {
            emptyList()
        }
    }

    override fun findById(id: String): T? {
        val hit = es8Api.get(index, id)
        return resultConverter.convertItem(hit)
    }

    override fun exists(id: String): Boolean {
        return try {
            es8Api.documentExists(index, id)
            true
        } catch (_ : Exception) {
            false
        }
    }

    // TODO: We can use es8 upsert api too
    override fun upsert(id: String, entity: T): T? = this.save(id, entity)

    override fun upsertBatch(entities: List<T>): List<T> = this.saveBatch(entities)

    private fun encode(obj: T): JsonNode? {
        return docMapper.toDoc(obj)
    }

    override fun streamRows(
        fields: List<Field>,
        query: Query,
        sorts: List<DocSort>,
        params: Map<String, Any>
    ): Iterator<List<Any?>> {
        val request = SearchRequest(fields = fields, query = query, sorts = sorts, params = params)
        return RowScrollIterator(request)
    }

    override fun streamItems(query: Query, sorts: List<DocSort>, params: Map<String, Any>): Iterator<T> {
        val request = SearchRequest(query = query, sorts = sorts, params = params)
        return ItemScrollIterator(request)
    }

    internal inner class ItemScrollIterator(
        private val request: SearchRequest,
        result: SearchResult<T>? = null,
        private val scroll: String = "1m"
    ) : Iterator<T> {

        private var queue: ArrayDeque<T>
        private var scrollId: String?

        init {
            if (result == null) {
                val esReq = requestConverter.convert(request.copy(offset = 0, limit = 100))
                val esResult = es8Api.search(index, esReq)
                val items = resultConverter.convert(request, es8Api.search(index, scroll, esReq)).items
                queue = ArrayDeque(items)
                scrollId = esResult.scrollId
                // initialize
            } else {
                queue = ArrayDeque(result.items)
                scrollId = result.headers["scrollId"]?.toString()
            }
        }

        override fun next(): T {
            if (!hasNext()) {
                throw NoSuchElementException()
            } else {
                return queue.pop()
            }
        }

        override fun hasNext(): Boolean {
            return if (queue.isNotEmpty()) {
                true
            } else {
                val sid = scrollId
                if (sid != null) {
                    val esRes = es8Api.scroll(scroll, sid)
                    val result = resultConverter.convert(request, esRes)
                    val items: List<T> = requireNotNull(result.items)
                    queue = ArrayDeque(items)
                    // set to null if we're at the end
                    scrollId = if (queue.isEmpty()) null else result.headers["scrollId"] as String
                }
                !queue.isEmpty()
            }
        }
    }

    internal inner class RowScrollIterator(
        private val request: SearchRequest,
        result: SearchResult<T>? = null,
        private val scroll: String = "1m"
    ) : Iterator<List<Any?>> {

        private var queue: ArrayDeque<List<Any?>>
        private var scrollId: String?

        init {
            if (result == null) {
                val esReq = requestConverter.convert(request.copy(offset = 0, limit = 100))
                val esResult = es8Api.search(index, esReq)
                val rows = resultConverter.convert(request, es8Api.search(index, scroll, esReq)).rows
                queue = ArrayDeque(rows)
                scrollId = esResult.scrollId
                // initialize
            } else {
                queue = ArrayDeque(result.rows)
                scrollId = result.headers["scrollId"]?.toString()
            }
        }

        override fun next(): List<Any?> {
            if (!hasNext()) {
                throw NoSuchElementException()
            } else {
                return queue.pop()
            }
        }

        override fun hasNext(): Boolean {
            return if (queue.isNotEmpty()) {
                true
            } else {
                val sid = scrollId
                if (sid != null) {
                    val esRes = es8Api.scroll(scroll, sid)
                    val result = resultConverter.convert(request, esRes)
                    val rows: List<List<Any?>> = requireNotNull(result.rows)
                    queue = ArrayDeque(rows)
                    // set to null if we're at the end
                    scrollId = if (queue.isEmpty()) null else result.headers["scrollId"] as String
                }
                !queue.isEmpty()
            }
        }
    }

}
