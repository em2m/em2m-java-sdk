package io.em2m.search.es8

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import feign.Headers
import feign.Param
import feign.RequestLine
import io.em2m.search.es.models.EsStatus
import io.em2m.search.es.EsAliasAction
import io.em2m.search.es.EsAliasDefinition
import io.em2m.search.es.EsAliasRequest
import io.em2m.search.es.EsExistsQuery
import io.em2m.search.es.EsQuery
import io.em2m.search.es.EsSearchRequest
import io.em2m.search.es8.models.index.Es8Mapping
import io.em2m.search.es8.models.index.Es8MappingProperty
import io.em2m.search.es8.models.Es8Stats
import io.em2m.search.es8.models.auth.*
import io.em2m.search.es8.models.bulk.Es8BulkResult
import io.em2m.search.es8.models.index.Es8IndexTemplates
import io.em2m.search.es8.models.index.component.Es8ComponentTemplates
import io.em2m.search.es8.models.search.Es8Hit
import io.em2m.search.es8.models.search.Es8SearchResult

// TODO: Replace with elasticsearch dependency
@Headers("Content-Type: application/json")
interface Es8Api {

    @RequestLine("POST /{index}/_search", decodeSlash = false)
    fun search(@Param("index") index: String, request: EsSearchRequest = EsSearchRequest()): Es8SearchResult

    @RequestLine(value = "POST /{index}/_search?scroll={scroll}", decodeSlash = false)
    fun search(@Param("index") index: String, @Param("scroll") scroll: String, request: EsSearchRequest): Es8SearchResult

    @RequestLine(value = "GET /_search/scroll?scroll={scroll}&scroll_id={scrollId}", decodeSlash = false)
    fun scroll(@Param("scroll") scroll: String, @Param("scrollId") scrollId: String): Es8SearchResult

    @RequestLine(value = "PUT /{index}/", decodeSlash = false)
    fun createIndex(@Param("index") index: String)

    @RequestLine(value = "PUT /{index}/", decodeSlash = false)
    fun createIndex(@Param("index") index: String, settings: ObjectNode)

    @RequestLine(value = "HEAD /{index}/", decodeSlash = false)
    fun indexExists(@Param("index") index: String)

    @RequestLine(value = "HEAD /{index}/_doc/{id}", decodeSlash = false)
    fun documentExists(@Param("index") index: String, @Param("id") id: String)

    @RequestLine(value = "GET /{index}/_mapping", decodeSlash = false)
    fun getMappings(@Param("index") index: String): ObjectNode

    @RequestLine(value = "GET /{index}/_mapping", decodeSlash = false)
    fun getMapping(@Param("index") index: String): Map<String, Any?>

    @RequestLine(value = "PUT /{index}/_mapping/", decodeSlash = false)
    fun putMapping(@Param("index") index: String, mapping: ObjectNode)

    @RequestLine(value = "PUT /{index}/_doc/{id}", decodeSlash = false)
    fun put(@Param("index") index: String, @Param("id") id: String, document: Any)

    @RequestLine(value = "GET /{index}/_doc/{id}", decodeSlash = false)
    fun get(@Param("index") index: String, @Param("id") id: String): Es8Hit

    @Deprecated("Prefer the elasticsearch BulkRequest model over raw strings.")
    @Headers("Content-Type: application/x-ndjson")
    @RequestLine("POST /_bulk")
    fun bulkUpdate(bulkRequest: String): Es8BulkResult

    @RequestLine("POST /_flush")
    fun flush()

    @RequestLine("GET /_cluster/state/metadata", decodeSlash = false)
    fun getMetadata(): ObjectNode

    @RequestLine("GET /", decodeSlash = false)
    fun getStatus(): EsStatus

    @RequestLine("POST /_aliases", decodeSlash = false)
    fun putAliases(request: EsAliasRequest)

    @RequestLine("GET /_aliases", decodeSlash = false)
    fun getAliases(): ObjectNode

    @RequestLine("DELETE /{index}/_doc/{id}", decodeSlash = false)
    fun delete(@Param("index") index: String, @Param("id") id: String)

    @Deprecated("Please use caution when deleting indices or dropping tables.")
    @RequestLine("DELETE /{index}", decodeSlash = false)
    fun deleteIndex(@Param("index") index: String)

    @RequestLine("POST /{index}/_delete_by_query")
    fun deleteByQuery(@Param("index") index: String, query: EsQuery)

    @RequestLine("GET /_cat/indices?format=json", decodeSlash = false)
    fun getIndices(): List<ObjectNode>

    @RequestLine("GET /{index}/_settings?include_defaults=true", decodeSlash = false)
    fun getSettings(@Param("index") index: String): Map<String, Any?>

    @RequestLine("GET /{index}/_stats", decodeSlash = false)
    fun getStats(@Param("index") index: String): Es8Stats

    @RequestLine("GET /_stats", decodeSlash = false)
    fun getStats(): ObjectNode

    // <editor-fold desc="Auth">
    @RequestLine("GET /_security/user/_has_privileges", decodeSlash = false)
    fun hasPrivileges(request: Es8HasPrivilegesRequest): Es8HasPrivilegesResponse

    @RequestLine("GET /_security/user", decodeSlash = false)
    fun getUsers(): Map<String, Es8User>

    @RequestLine("DELETE /_security/user/{username}", decodeSlash = false)
    fun deleteUser(@Param("username") username: String): Es8DeleteUserResponse

    @RequestLine("PUT /_security/user/{username}/_disable", decodeSlash = false)
    fun disableUser(@Param("username") username: String): ObjectNode

    @RequestLine("PUT /_security/user/{username}/_enable", decodeSlash = false)
    fun enableUser(@Param("username") username: String): ObjectNode

    @RequestLine("POST /_security/user/{username}", decodeSlash = false)
    fun createUser(@Param("username") username: String, request: Es8CreateUserRequest): Es8CreatedResponse

    @RequestLine("POST /_security/role/{roleName}", decodeSlash = false)
    fun createRole(@Param("roleName") roleName: String, request: Es8Role): Es8CreatedResponse

    @RequestLine("GET /_security/role", decodeSlash = false)
    fun listRoles(): Map<String, ObjectNode>

    @RequestLine("DELETE /_security/role/{roleName}")
    fun deleteRole(@Param("roleName") roleName: String): ObjectNode

    // </editor-fold>

    // <editor-fold desc="Templates">
    @RequestLine("GET /_index_template")
    fun getIndexTemplates(): Es8IndexTemplates

    @RequestLine("GET _index_template/{template}?filter_path=index_templates.name,index_templates.index_template.index_patterns,index_templates.index_template.data_stream")
    fun getIndexTemplate(@Param("template") template: String): Es8IndexTemplates

    @RequestLine("GET /_component_template")
    fun getComponentTemplates(): Es8ComponentTemplates

    @RequestLine("GET /_component_template/{template}")
    fun getComponentTemplate(@Param("template") template: String): Es8ComponentTemplates
    // </editor-fold>

}
