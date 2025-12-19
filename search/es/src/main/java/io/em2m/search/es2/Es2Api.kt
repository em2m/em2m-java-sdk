package io.em2m.search.es2

import com.fasterxml.jackson.databind.node.ObjectNode
import feign.Headers
import feign.Param
import feign.RequestLine
import io.em2m.search.es.models.EsStatus
import io.em2m.search.es.*
import io.em2m.search.es2.models.Es2Index
import io.em2m.search.es2.models.Es2Stats
import kotlin.collections.component1
import kotlin.collections.component2

@Headers("Content-Type: application/json")
@Deprecated("Migrate to Es8Api", ReplaceWith("io.em2m.search.es8.Es8Api"))
interface Es2Api {

    @RequestLine(value = "POST /{index}/{type}/_search", decodeSlash = false)
    fun search(@Param("index") index: String, @Param("type") type: String, request: EsSearchRequest): EsSearchResult

    @RequestLine(value = "POST /{index}/{type}/_search?scroll={scroll}", decodeSlash = false)
    fun search(@Param("index") index: String, @Param("type") type: String, @Param("scroll") scroll: String, request: EsSearchRequest): EsSearchResult

    @RequestLine(value = "GET /_search/scroll?scroll={scroll}&scroll_id={scrollId}", decodeSlash = false)
    fun scroll(@Param("scroll") scroll: String, @Param("scrollId") scrollId: String): EsSearchResult

    @RequestLine(value = "PUT /{index}/", decodeSlash = false)
    fun createIndex(@Param("index") index: String)

    @RequestLine(value = "PUT /{index}/", decodeSlash = false)
    fun createIndex(@Param("index") index: String, settings: ObjectNode)

    @RequestLine(value = "HEAD /{index}/", decodeSlash = false)
    fun indexExists(@Param("index") index: String)

    @RequestLine(value = "GET /{index}/_mapping", decodeSlash = false)
    fun getMappings(@Param("index") index: String): ObjectNode

    @RequestLine(value = "HEAD /{index}/{type}", decodeSlash = false)
    fun mappingExists(@Param("index") index: String, @Param("type") type: String)

    @RequestLine(value = "GET /{index}/_mapping/{type}", decodeSlash = false)
    fun getMapping(@Param("index") index: String, @Param("type") type: String): ObjectNode

    @RequestLine(value = "GET /{index}/_mapping?format=json", decodeSlash = false)
    fun getMapping(@Param("index") index: String): Map<String, Any?>

    @RequestLine(value = "PUT /{index}/_mapping/{type}", decodeSlash = false)
    fun putMapping(@Param("index") index: String, @Param("type") type: String, mapping: ObjectNode)

    @RequestLine(value = "POST /{index}/{type}/{id}", decodeSlash = false)
    fun put(@Param("index") index: String, @Param("type") type: String, @Param("id") id: String, document: Any)

    @RequestLine(value = "GET /{index}/{type}/{id}", decodeSlash = false)
    fun get(@Param("index") index: String, @Param("type") type: String, @Param("id") id: String): EsHit

    @Headers("Content-Type: application/x-ndjson")
    @RequestLine("POST /_bulk")
    fun bulkUpdate(bulkRequest: String): EsBulkResult

    @RequestLine("POST /_flush")
    fun flush()

    @RequestLine("GET /_cluster/state/metadata")
    fun getMetadata(): ObjectNode

    @RequestLine("GET /")
    fun getStatus(): EsStatus

    @RequestLine("POST /_aliases")
    fun putAliases(request: EsAliasRequest)

    @RequestLine("GET /_aliases")
    fun getAliases(): ObjectNode

    @RequestLine("GET /_cat/indices?format=json", decodeSlash = false)
    fun getIndices(): List<Es2Index>

    @RequestLine("GET /{index}/_settings?include_defaults=true", decodeSlash = false)
    fun getSettings(@Param("index") index: String): Map<String, Any?>

    @RequestLine("GET /{index}/_stats", decodeSlash = false)
    fun getStats(@Param("index") index: String): Es2Stats

    @RequestLine("GET /_stats", decodeSlash = false)
    fun getStats(): Es2Stats

}
