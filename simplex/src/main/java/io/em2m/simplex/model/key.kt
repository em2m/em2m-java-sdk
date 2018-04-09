package io.em2m.simplex.model


data class Key(val namespace: String, val name: String) {

    private val key = namespace + ":" + name

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true;
        }
        if (other is Key) {
            return this.key == other.key
        }
        return false
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    companion object {
        fun parse(key: String): Key {
            val parts = key.split(":")
            return if (parts.size == 1) {
                Key("field", parts[0])
            } else if (parts.size == 2) {
                Key(parts[0], parts[1])
            } else throw IllegalArgumentException("Invalid Key: $key")
        }
    }
}

interface KeyHandler {

    fun call(key: Key, context: ExprContext): Any?

}

abstract class KeyHandlerSupport : KeyHandler {

}

class ConstKeyHandler(val value: Any?) : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return value
    }

}

interface KeyResolver {

    fun find(key: Key): KeyHandler?

}

class BasicKeyResolver(handlers: Map<Key, KeyHandler> = emptyMap(), vararg delegates: KeyResolver) : KeyResolver {

    private val delegates = ArrayList<KeyResolver>()

    private val handlers = HashMap<Key, (Key) -> KeyHandler>()

    init {
        handlers.forEach {
            this.handlers.put(it.key, { _ -> it.value })
        }
        this.delegates.addAll(delegates)
    }

    fun delegate(resolver: KeyResolver): BasicKeyResolver {
        delegates.add(resolver)
        return this
    }

    fun key(key: Key, handler: (Key) -> KeyHandler): BasicKeyResolver {
        handlers[key] = handler
        return this
    }

    fun key(key: Key, handler: KeyHandler): BasicKeyResolver {
        handlers.put(key, { _ -> handler })
        return this
    }

    override fun find(key: Key): KeyHandler? {
        var result = (handlers[key] ?: handlers[key.copy(name = "*")])?.invoke(key)
        if (result != null) return result
        for (delegate in delegates) {
            result = delegate.find(key)
            if (result != null) break
        }
        return result
    }

}

