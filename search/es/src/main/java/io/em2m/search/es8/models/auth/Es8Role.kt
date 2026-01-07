package io.em2m.search.es8.models.auth

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.node.ObjectNode
import io.em2m.search.es.EsQuery

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class Es8AuthIndexAccess(
    val names: List<String> = listOf("*"),
    val privileges: List<String> = listOf("none"),
    val field_security : Map<String, Any> = emptyMap(),
    val query: EsQuery? = null) {

    init {
        if (privileges.any { indexPrivilege -> indexPrivilege !in INDEX_PRIVILEGES }) {
            throw IllegalArgumentException("Unrecognized index privilege")
        }
    }

    companion object {
        val DEFAULT = Es8AuthIndexAccess(
            names = listOf("*"),
            privileges = ENGINEER_INDEX_PRIVILEGES
        )
    }

}

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class Es8Role(val description: String,
                   val cluster: List<String> = listOf("none"),
                   val indices: List<Es8AuthIndexAccess> = listOf(Es8AuthIndexAccess.DEFAULT),
                   val applications: List<ObjectNode>? = null,
                   val metadata: ObjectNode? = null) {

    init {
        if (cluster.any { clusterPrivilege -> clusterPrivilege !in CLUSTER_PRIVILEGES }) {
            throw IllegalArgumentException("Unrecognized cluster privilege")
        }
    }

}
