package io.em2m.search.es

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.core.daos.AbstractSyncDao
import io.em2m.search.core.model.*
import java.util.*

class EsSyncDao<T>(val esApi: EsApi, val index: String, val type: String, tClass: Class<T>, idMapper: IdMapper<T>, docMapper: DocMapper<T>? = null) :
        AbstractSyncDao<T>(idMapper) {

    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
    val docMapper: DocMapper<T> = docMapper ?: JacksonDocMapper(tClass, objectMapper)
    val requestConverter = RequestConverter(objectMapper)
    val resultConverter = ResultConverter(this.docMapper)

    override fun create(entity: T): T? {
        val id = generateId()
        idMapper.setId(entity, id)
        val doc = encode(entity)
        return if (doc != null) {
            esApi.put(index, type, generateId(), doc)
            entity
        } else null
    }

    override fun deleteById(id: String): Boolean {
        esApi.delete(index, type, id)
        // TODO - Find any efficient way to determine if entity was deleted
        return true
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        val esReq = requestConverter.convert(request)
        val scroll = request.params["scroll"] as String?
        val result = if (scroll != null)
            resultConverter.convert(request, esApi.search(index, type, scroll, esReq))
        else
            resultConverter.convert(request, esApi.search(index, type, esReq))
        return result
    }

    fun scrollItems(request: SearchRequest, result: SearchResult<T>): Iterable<T> {
        // TODO - Consider creating a different request signature or validating search request is compatible
        val scroll = request.params["scroll"] as String
        val iterable = object : Iterable<T> {
            override fun iterator(): Iterator<T> {
                return ItemScrollIterator(request, result, scroll)
            }
        }
        return iterable
    }

    fun scrollRows(request: SearchRequest, result: SearchResult<T>): Iterable<Any?> {
        // TODO - Consider creating a different request signature or validating search request is compatible
        val scroll = request.params["scroll"] as String
        val iterable = object : Iterable<List<Any?>> {
            override fun iterator(): Iterator<List<Any?>> {
                return RowScrollIterator(request, result, scroll)
            }
        }
        return iterable
    }

    override fun save(id: String, entity: T): T? {
        idMapper.setId(entity, id)
        val doc = docMapper.toDoc(entity)
        return if (doc != null) {
            esApi.put(index, type, id, doc)
            entity
        } else null
    }

    private fun bulkIndex(index: String, type: String, id: String, entity: T): String {
        val line1 = """{ "index": {"_index": "$index", "_type": "$type", "_id": "$id"} }"""
        val line2 = objectMapper.writeValueAsString(docMapper.toDoc(entity))
        return "$line1\n$line2\n"
    }

    override fun saveBatch(entities: List<T>): List<T> {
        val bulkRequest = entities.map { bulkIndex(index, type, idMapper.getId(it), it) }.joinToString("")
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

    internal fun encode(obj: T): JsonNode? {
        return docMapper.toDoc(obj)
    }

    internal fun scroll(request: SearchRequest, scroll: String, scrollId: String): SearchResult<T> {
        val esRes = esApi.scroll(scroll, scrollId)
        return resultConverter.convert(request, esRes)
    }

    internal inner class ItemScrollIterator(val request: SearchRequest, result: SearchResult<T>, val scroll: String = "1m") : Iterator<T> {

        var queue: ArrayDeque<T> = ArrayDeque(result.items)
        var scrollId = result.headers["scrollId"] as String?

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

    internal inner class RowScrollIterator(val request: SearchRequest, result: SearchResult<T>, val scroll: String = "1m") : Iterator<List<Any?>> {

        var queue: ArrayDeque<List<Any?>> = ArrayDeque(result.rows)
        var scrollId = result.headers["scrollId"] as String?

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