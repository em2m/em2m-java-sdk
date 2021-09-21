package io.em2m.search.es

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.core.daos.AbstractSyncDao
import io.em2m.search.core.model.*
import java.util.*

class EsSyncDao<T>(
    val esApi: EsApi,
    val index: String,
    val type: String,
    tClass: Class<T>,
    idMapper: IdMapper<T>,
    docMapper: DocMapper<T>? = null
) :
    AbstractSyncDao<T>(idMapper), StreamableDao<T> {

    private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
    private val docMapper: DocMapper<T> = docMapper ?: JacksonDocMapper(tClass, objectMapper)
    val requestConverter = RequestConverter(objectMapper)
    val resultConverter = ResultConverter(this.docMapper)

    private fun String.urlEncode() = java.net.URLEncoder.encode(this, "UTF-8")

    override fun create(entity: T): T? {
        val id = generateId()
        val transformedEntity = idMapper.setId(entity, id)
        val doc = encode(transformedEntity)
        return if (doc != null) {
            esApi.put(index, type, idMapper.getId(transformedEntity).urlEncode(), doc)
            entity
        } else null
    }

    override fun deleteById(id: String): Boolean {
        // TODO: DOES NOT SUPPORT ID MAPPER TRANSFORMED ENTITIES
        esApi.delete(index, type, id.urlEncode())
        // TODO - Find any efficient way to determine if entity was deleted
        return true
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        val esReq = requestConverter.convert(request)
        val scroll = request.params["scroll"] as String?
        return if (scroll != null)
            resultConverter.convert(request, esApi.search(index, type, scroll, esReq))
        else
            resultConverter.convert(request, esApi.search(index, type, esReq))
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
            esApi.put(index, type, idMapper.getId(entity).urlEncode(), doc)
            entity
        } else null
    }

    private fun bulkIndex(index: String, type: String, id: String, entity: T): String {
        val line1 = """{ "index": {"_index": "$index", "_type": "$type", "_id": "$id"} }"""
        val line2 = objectMapper.writeValueAsString(docMapper.toDoc(entity))
        return "$line1\n$line2\n"
    }

    override fun saveBatch(entities: List<T>): List<T> {
        val bulkRequest = entities.joinToString("") { bulkIndex(index, type, idMapper.getId(it), it) }
        val bulkResult = esApi.bulkUpdate(bulkRequest)
        return if (bulkResult.errors) {
            val errorIds = bulkResult.items.filter {
                (it.index?.status ?: 0) >= 400
            }.map { it.index?._id }.toSet()
            entities.filter { errorIds.contains(idMapper.getId(it)) }
        } else {
            emptyList()
        }
    }

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
                val esResult = esApi.search(index, type, esReq)
                val items = resultConverter.convert(request, esApi.search(index, type, scroll, esReq)).items
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
                    val esRes = esApi.scroll(scroll, sid)
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
                val esResult = esApi.search(index, type, esReq)
                val rows = resultConverter.convert(request, esApi.search(index, type, scroll, esReq)).rows
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
                    val esRes = esApi.scroll(scroll, sid)
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
