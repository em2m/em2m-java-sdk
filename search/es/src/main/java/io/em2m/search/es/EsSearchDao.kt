package io.em2m.search.es

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.scaleset.geo.geojson.GeoJsonModule
import io.em2m.search.core.daos.AbstractSearchDao
import io.em2m.search.core.model.*
import rx.Observable
import rx.Observable.just
import java.util.*

class EsSearchDao<T>(val esApi: EsApi, val index: String, val type: String, tClass: Class<T>, idMapper: IdMapper<T>, docMapper: DocMapper<T>? = null) :
        AbstractSearchDao<T>(idMapper) {

    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
    val docMapper: DocMapper<T> = docMapper ?: JacksonDocMapper(tClass, objectMapper)
    val requestConverter = RequestConverter(objectMapper)
    val resultConverter = ResultConverter(this.docMapper)

    override fun create(entity: T): Observable<T> {
        val id = generateId()
        idMapper.setId(entity, id)
        val doc = encode(entity)
        return if (doc != null) {
            esApi.put(index, type, generateId(), doc)
            just(entity)
        } else just(null)
    }

    override fun deleteById(id: String): Observable<Boolean> {
        esApi.delete(index, type, id)
        // TODO - Find any efficient way to determine if entity was deleted
        return just(true)
    }

    override fun search(request: SearchRequest): Observable<SearchResult<T>> {
        val esReq = requestConverter.convert(request)
        val scroll = request.headers["scroll"] as String?
        val result = if (scroll != null)
            resultConverter.convert(request, esApi.search(index, type, scroll, esReq))
        else
            resultConverter.convert(request, esApi.search(index, type, esReq))
        return just(result)
    }

    fun scrollItems(request: SearchRequest, result: SearchResult<T>): Observable<T> {
        // TODO - Consider creating a different request signature or validating search request is compatible
        val scroll = request.headers["scroll"] as String
        val iterable = object : Iterable<T> {
            override fun iterator(): Iterator<T> {
                return ItemScrollIterator(request, result, scroll)
            }
        }
        return Observable.from(iterable)
    }

    fun scrollRows(request: SearchRequest, result: SearchResult<T>): Observable<List<Any?>> {
        // TODO - Consider creating a different request signature or validating search request is compatible
        val scroll = request.headers["scroll"] as String
        val iterable = object : Iterable<List<Any?>> {
            override fun iterator(): Iterator<List<Any?>> {
                return RowScrollIterator(request, result, scroll)
            }
        }
        return Observable.from(iterable)
    }

    override fun save(id: String, entity: T): Observable<T> {
        idMapper.setId(entity, id)
        val doc = docMapper.toDoc(entity)
        return if (doc != null) {
            esApi.put(index, type, id, doc)
            just(entity)
        } else just(null)
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