package io.em2m.simplex.model


data class Key(val namespace: String, val name: String) {

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

interface KeyResolver {

    fun find(key: Key): KeyHandler?

}

abstract class KeyHandlerSupport : KeyHandler {

}

class ConstKeyHandler(val value: Any?) : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return value
    }

}
