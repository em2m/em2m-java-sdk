package io.em2m.actions.model

import io.em2m.policy.model.ActionCheck
import io.em2m.policy.model.Claims
import io.em2m.policy.model.Environment
import io.em2m.policy.model.PolicyContext
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.KeyHandler
import java.io.InputStream
import java.util.*
import kotlin.collections.HashMap

data class ActionContext(val actionName: String,
                         var claims: Claims = Claims(emptyMap()),
                         var environment: MutableMap<String, Any?> = HashMap(),
                         var requestId: String = UUID.randomUUID().toString(),
                         var resource: String? = null,
                         var inputStream: InputStream? = null,
                         var request: Any? = null,
                         var multipart: MultipartData? = null,
                         var actionCheck: ActionCheck? = null,
                         var rewrites: Map<String, Any?> = emptyMap(),
                         val scope: MutableMap<String, Any?> = HashMap(),
                         var debug: Boolean = false,
                         var error: Throwable? = null,
                         val response: Response) : ActionFlowAware {

    constructor(actionName: String,
                inputStream: InputStream? = null,
                claims: Map<String, Any?> = emptyMap(),
                environment: MutableMap<String, Any?> = HashMap(),
                resource: String? = null,
                actionCheck: ActionCheck? = null,
                rewrites: Map<String, Any?> = emptyMap(),
                scope: MutableMap<String, Any?> = HashMap(),
                debug: Boolean = false,
                requestId: String = UUID.randomUUID().toString(),
                request: Any? = null,
                multipart: MultipartData? = null,
                error: Throwable? = null,
                response: Response) : this(actionName, Claims(claims), environment, requestId, resource, inputStream, request, multipart, actionCheck, rewrites, scope, debug, error, response)

    val keyHandlers = HashMap<Key, KeyHandler>()

    override var flow: ActionFlow? = null

    fun toPolicyContext(): PolicyContext {
        val keyResolver = BasicKeyResolver(keyHandlers)
        return PolicyContext(mapOf("actionContext" to this), claims, Environment(environment), resource, keyResolver)
    }

}
