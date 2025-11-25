package io.em2m.search.es8.models.auth

import com.fasterxml.jackson.annotation.JsonProperty

data class Es8HasPrivilegesResponse(val username: String,
                                    @param:JsonProperty("has_all_requested") val hasAllRequested: Boolean,
                                    val index: Map<String, Any?> = emptyMap(),
                                    val cluster: Map<String, Any?> = emptyMap(),
                                    val application: Map<String, Any?> = emptyMap())
