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

    @RequestLine("GET /{index}/_search", decodeSlash = false)
    fun search(@Param("index") index: String, request: EsSearchRequest = EsSearchRequest()): Es8SearchResult

    @RequestLine(value = "POST /{index}/_search?scroll={scroll}", decodeSlash = false)
    fun search(@Param("index") index: String, @Param("scroll") scroll: String, request: EsSearchRequest): Es8SearchResult

    @RequestLine(value = "GET /_search/scroll?scroll={scroll}&scroll_id={scrollId}", decodeSlash = false)
    fun scroll(@Param("scroll") scroll: String, @Param("scrollId") scrollId: String): Es8SearchResult

    fun searchMalformed(index: String): Es8SearchResult {
        return search(index, EsSearchRequest( query =  EsExistsQuery(field = "_ignored")))
    }

    fun searchMalformed(index: String, scroll: String): Es8SearchResult {
        return search(index, scroll, EsSearchRequest( query =  EsExistsQuery(field = "_ignored")))
    }

    @RequestLine(value = "PUT /{index}/", decodeSlash = false)
    fun createIndex(@Param("index") index: String)

    @RequestLine(value = "PUT /{index}/", decodeSlash = false)
    fun createIndex(@Param("index") index: String, settings: ObjectNode)

    fun canCreateIndex(index: String): Boolean = this.hasIndexPrivilege(index, CREATE_INDEX_PRIVILEGE)

    @RequestLine(value = "HEAD /{index}/", decodeSlash = false)
    fun indexExists(@Param("index") index: String)

    fun exists(index: String): Boolean = try {
        indexExists(index)
        true
    } catch (_: Exception) {
        false
    }

    @RequestLine(value = "HEAD /{index}/_doc/{id}", decodeSlash = false)
    fun documentExists(@Param("index") index: String, @Param("id") id: String)

    fun exists(index: String, id: String): Boolean = try {
        documentExists(index, id)
        true
    } catch (_: Exception) {
        false
    }

    @RequestLine(value = "GET /{index}/_mapping", decodeSlash = false)
    fun getMappings(@Param("index") index: String): ObjectNode

    @RequestLine(value = "GET /{index}/_mapping", decodeSlash = false)
    fun getMapping(@Param("index") index: String): Map<String, Any?>

    @RequestLine(value = "PUT /{index}/_mapping/", decodeSlash = false)
    fun putMapping(@Param("index") index: String, mapping: ObjectNode)

    fun putMapping(index: String, mapping: Es8MappingProperty, mapper: ObjectMapper = DEFAULT_OBJECT_MAPPER) {
        val mappingNode = mapper.valueToTree<ObjectNode>(mapping)
        mappingNode.removeAll(JsonNode::isNull)
        putMapping(index, mappingNode)
    }

    fun putMapping(mapping: Es8Mapping) {
        this.putMapping(mapping.index, mapping.properties)
    }

    @RequestLine(value = "PUT /{index}/_doc/{id}", decodeSlash = false)
    fun put(@Param("index") index: String, @Param("id") id: String, document: Any)

    @RequestLine(value = "GET /{index}/{id}", decodeSlash = false)
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

    fun addAlias(index: String, vararg aliases: String) {
        if (aliases.isEmpty()) return
        val request = EsAliasRequest()
        request.actions = aliases.toSet().mapTo(mutableListOf()) { alias ->
            EsAliasAction(
                add= EsAliasDefinition(index, alias)
            )
        }
        this.putAliases(request)
    }

    fun removeAlias(index: String, vararg aliases: String) {
        val request = EsAliasRequest()
        request.actions = aliases.toSet().mapTo(mutableListOf()) { alias ->
            EsAliasAction(
                remove= EsAliasDefinition(index, alias)
            )
        }
        this.putAliases(request)
    }

    fun swapAliases(oldIndex: String, newIndex: String, alias: String) {
        val request = EsAliasRequest()
        request.actions = mutableListOf(EsAliasAction(
            add = EsAliasDefinition(newIndex, alias),
            remove = EsAliasDefinition(oldIndex, alias)
        ))
        this.putAliases(request)
    }

    @RequestLine("GET /_aliases", decodeSlash = false)
    fun getAliases(): ObjectNode

    fun getIndicesToAliases(): Map<String, List<String>> {
        val allAliases = getAliases()
        return allAliases.fieldNames().asSequence()
            .filterNot { it.startsWith(".") } // es-defined hidden aliases
            .associateWith { indexName ->
                allAliases.get(indexName).get("aliases").fieldNames()
                    .asSequence()
                    .toList()
            }
    }

    fun getAliases(index: String): List<String> {
        val indicesToAliases = getIndicesToAliases()
        return indicesToAliases[index] ?: emptyList()
    }

    @RequestLine("DELETE /{index}/_doc/{id}", decodeSlash = false)
    fun delete(@Param("index") index: String, @Param("id") id: String)

    @Deprecated("Please use caution when deleting indices or dropping tables.")
    @RequestLine("DELETE /{index}", decodeSlash = false)
    fun deleteIndex(@Param("index") index: String)

    fun canDeleteIndex(index: String): Boolean = this.hasIndexPrivilege(index, DELETE_INDEX_PRIVILEGE)

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

    fun hasIndexPrivilege(index: String, privilege: String): Boolean {
        val request = Es8HasPrivilegesRequest(
            cluster = listOf("monitor"),
            index= Es8AuthIndexAccess(names = listOf(index), privileges= listOf(privilege)))
        return try {
            hasPrivileges(request).hasAllRequested
        } catch (_ : Exception) {
            false
        }
    }

    fun hasIndexPrivileges(index: String, vararg privileges: String): Boolean {
        val request = Es8HasPrivilegesRequest(
            cluster = listOf("monitor"),
            index= Es8AuthIndexAccess(names = listOf(index), privileges= privileges.toList()))
        return try {
            hasPrivileges(request).hasAllRequested
        } catch (_ : Exception) {
            false
        }
    }

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

    fun createUser(request: Es8CreateUserRequest): Es8CreatedResponse = this.createUser(
        username= Es8CreateUserRequest.getDefaultUsername(request),
        request= request)

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

    companion object {
        private val DEFAULT_OBJECT_MAPPER: ObjectMapper = jacksonObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    }

}
