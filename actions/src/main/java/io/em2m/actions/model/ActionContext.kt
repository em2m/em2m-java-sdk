package io.em2m.actions.model

import io.em2m.policy.model.Claims
import io.em2m.policy.model.Environment
import io.em2m.policy.model.PolicyContext
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.KeyHandler
import java.io.InputStream
import java.util.*
import javax.servlet.http.Part

data class ActionContext(val actionName: String,
                         val inputStream: InputStream? = null, val parts: List<Part> = emptyList(),
                         var claims: Map<String, Any?> = emptyMap(), var environment: Map<String, Any?> = emptyMap(), var resource: String? = null,
                         val scope: MutableMap<String, Any?> = HashMap(),
                         var debug: Boolean = false,
                         var requestId: String = UUID.randomUUID().toString(),
                         var request: Any? = null,
                         var multipart: MultipartData? = null,
                         val response: Response = Response()) {

    val keyHandlers = HashMap<Key, KeyHandler>()
    private val keyResolver = BasicKeyResolver(keyHandlers)

    fun toPolicyContext(): PolicyContext {

        return PolicyContext(mapOf("actionContext" to this), Claims(claims), Environment(environment), resource, keyResolver)
    }

}
