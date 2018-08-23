package io.em2m.actions.model

import io.em2m.flows.Flow
import io.em2m.flows.FlowAware
import io.em2m.policy.model.Claims
import io.em2m.policy.model.Environment
import io.em2m.policy.model.PolicyContext
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.KeyHandler
import java.io.InputStream
import java.util.*
import javax.servlet.http.Part
import kotlin.collections.HashMap

data class ActionContext(val actionName: String,
                         var inputStream: InputStream? = null,
                         val parts: List<Part> = emptyList(),
                         var claims: Map<String, Any?> = emptyMap(),
                         var environment: MutableMap<String, Any?> = HashMap(),
                         var resource: String? = null,
                         val scope: MutableMap<String, Any?> = HashMap(),
                         var debug: Boolean = false,
                         var requestId: String = UUID.randomUUID().toString(),
                         var request: Any? = null,
                         var multipart: MultipartData? = null,
                         var error: Throwable? = null,
                         val response: Response) : FlowAware {

    val keyHandlers = HashMap<Key, KeyHandler>()
    private val keyResolver = BasicKeyResolver(keyHandlers)

    override var flow: Flow<*>? = null

    fun toPolicyContext(): PolicyContext {

        return PolicyContext(mapOf("actionContext" to this), Claims(claims), Environment(environment), resource, keyResolver)
    }

}
