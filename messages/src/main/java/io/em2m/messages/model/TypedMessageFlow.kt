package io.em2m.messages.model

import io.em2m.flows.Transformer

open class TypedMessageFlow<T : Any>(val messageType: Class<out Any>) : MessageFlow {

    override val transformers = ArrayList<Transformer<MessageContext>>()

    @Suppress("UNCHECKED_CAST")
    fun message(context: MessageContext): T {
        val result = context.message as? T
        return requireNotNull(result)
    }

    override fun main(context: MessageContext) {
        val result = main(context, message(context))
    }

    open fun main(context: MessageContext, msg: T) {
    }

}