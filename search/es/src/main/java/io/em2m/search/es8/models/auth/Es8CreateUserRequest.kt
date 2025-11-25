package io.em2m.search.es8.models.auth

data class Es8CreateUserRequest(
    val password: String,
    val roles: List<String>,
    val full_name: String,
    val email: String,
    val metadata: Map<String, Any> = mutableMapOf(),
    val enabled: Boolean = true,
) {

    companion object {
        fun getDefaultUsername(request: Es8CreateUserRequest): String {
            return request.email.substringBefore("@")
        }
    }
}
