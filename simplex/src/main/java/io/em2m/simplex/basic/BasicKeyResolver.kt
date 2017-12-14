package io.em2m.simplex.basic

import io.em2m.simplex.model.Key
import io.em2m.simplex.model.KeyHandler
import io.em2m.simplex.model.KeyResolver

class BasicKeyResolver(val handlers: Map<Key, KeyHandler>) : KeyResolver {

    override fun find(key: Key): KeyHandler? {
        val result = handlers[key] ?: handlers[key.copy(name = "*")]
        return result
    }

}