package io.em2m.messages.model

import io.em2m.flows.Flow
import io.em2m.flows.FlowAware
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.KeyHandler
import java.io.InputStream

data class MessageContext(var inputStream: InputStream? = null,
                          var message: Any? = null,
                          val environment: MutableMap<String, Any?> = HashMap(),
                          val scope: MutableMap<String, Any?> = HashMap(),
                          var eventId: String? = null) : FlowAware {

    override var flow: Flow<*>? = null

    val keyHandlers = HashMap<Key, KeyHandler>()
    val keyResolver = BasicKeyResolver(keyHandlers)

    val channel: String?
        get() = environment["Channel"] as? String

}