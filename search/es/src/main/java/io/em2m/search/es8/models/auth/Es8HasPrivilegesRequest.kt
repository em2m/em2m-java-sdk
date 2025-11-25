package io.em2m.search.es8.models.auth

data class Es8HasPrivilegesRequest(
    val cluster: List<String>,
    val index: Es8AuthIndexAccess,
    val application: Map<String, Any?> = emptyMap())
