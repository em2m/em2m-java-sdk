package io.em2m.search.es8

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es.EsAliasAction
import io.em2m.search.es.EsAliasDefinition
import io.em2m.search.es.EsAliasRequest
import io.em2m.search.es.EsExistsQuery
import io.em2m.search.es.EsSearchRequest
import io.em2m.search.es8.models.auth.CREATE_INDEX_PRIVILEGE
import io.em2m.search.es8.models.auth.DELETE_INDEX_PRIVILEGE
import io.em2m.search.es8.models.auth.Es8AuthIndexAccess
import io.em2m.search.es8.models.auth.Es8CreateUserRequest
import io.em2m.search.es8.models.auth.Es8CreatedResponse
import io.em2m.search.es8.models.auth.Es8HasPrivilegesRequest
import io.em2m.search.es8.models.index.Es8Mapping
import io.em2m.search.es8.models.index.Es8MappingProperty
import io.em2m.search.es8.models.search.Es8SearchResult


fun Es8Api.exists(index: String): Boolean = try {
    indexExists(index)
    true
} catch (_: Exception) {
    false
}

fun Es8Api.exists(index: String, id: String): Boolean = try {
    documentExists(index, id)
    true
} catch (_: Exception) {
    false
}


fun Es8Api.canDeleteIndex(index: String): Boolean = run { this.hasIndexPrivilege(index, DELETE_INDEX_PRIVILEGE) }

fun Es8Api.putMapping(index: String, mapping: Es8MappingProperty, mapper: ObjectMapper = jacksonObjectMapper()) {
    val mappingNode = mapper.valueToTree<ObjectNode>(mapping)
    mappingNode.removeAll(JsonNode::isNull)
    putMapping(index, mappingNode)
}

fun Es8Api.putMapping(mapping: Es8Mapping) {
    this.putMapping(mapping.index, mapping.properties)
}

fun Es8Api.createUser(request: Es8CreateUserRequest): Es8CreatedResponse = this.createUser(
    username= Es8CreateUserRequest.getDefaultUsername(request),
    request= request)

fun Es8Api.hasIndexPrivilege(index: String, privilege: String): Boolean {
    val request = Es8HasPrivilegesRequest(
        cluster = listOf("monitor"),
        index= Es8AuthIndexAccess(names = listOf(index), privileges= listOf(privilege)))
    return try {
        hasPrivileges(request).hasAllRequested
    } catch (_ : Exception) {
        false
    }
}

fun Es8Api.hasIndexPrivileges(index: String, vararg privileges: String): Boolean {
    val request = Es8HasPrivilegesRequest(
        cluster = listOf("monitor"),
        index= Es8AuthIndexAccess(names = listOf(index), privileges= privileges.toList()))
    return try {
        hasPrivileges(request).hasAllRequested
    } catch (_ : Exception) {
        false
    }
}


fun Es8Api.getIndicesToAliases(): Map<String, List<String>> {
    val allAliases = getAliases()
    return allAliases.fieldNames().asSequence()
        .filterNot { it.startsWith(".") } // es-defined hidden aliases
        .associateWith { indexName ->
            allAliases.get(indexName).get("aliases").fieldNames()
                .asSequence()
                .toList()
        }
}

fun Es8Api.getAliases(index: String): List<String> {
    val indicesToAliases = getIndicesToAliases()
    return indicesToAliases[index] ?: emptyList()
}

fun Es8Api.addAlias(index: String, vararg aliases: String) {
    if (aliases.isEmpty()) return
    val request = EsAliasRequest()
    request.actions = aliases.toSet().mapTo(mutableListOf()) { alias ->
        EsAliasAction(
            add= EsAliasDefinition(index, alias)
        )
    }
    this.putAliases(request)
}

fun Es8Api.removeAlias(index: String, vararg aliases: String) {
    val request = EsAliasRequest()
    request.actions = aliases.toSet().mapTo(mutableListOf()) { alias ->
        EsAliasAction(
            remove= EsAliasDefinition(index, alias)
        )
    }
    this.putAliases(request)
}

fun Es8Api.swapAliases(oldIndex: String, newIndex: String, alias: String) {
    val request = EsAliasRequest()
    request.actions = mutableListOf(EsAliasAction(
        add = EsAliasDefinition(newIndex, alias),
        remove = EsAliasDefinition(oldIndex, alias)
    ))
    this.putAliases(request)
}


fun Es8Api.searchMalformed(index: String): Es8SearchResult {
    return search(index, EsSearchRequest( query =  EsExistsQuery(field = "_ignored")))
}

fun Es8Api.searchMalformed(index: String, scroll: String): Es8SearchResult {
    return search(index, scroll, EsSearchRequest( query =  EsExistsQuery(field = "_ignored")))
}

fun Es8Api.canCreateIndex(index: String): Boolean = this.hasIndexPrivilege(index, CREATE_INDEX_PRIVILEGE)
