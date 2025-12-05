package io.em2m.search.es8.models.auth

data class Es8User(val username: String,
                   val roles: List<String>,
                   val full_name: String?,
                   val email: String?,
                   val metadata: Map<String, Any?>,
                   val enabled: Boolean)
