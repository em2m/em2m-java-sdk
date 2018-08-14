package io.em2m.actions.model

import io.em2m.flows.Transformer

open class TypedActionFlow<T, R>(val requestType: Class<out Any>, val resultType: Class<out Any>) : ActionFlow {

    override val transformers = ArrayList<Transformer<ActionContext>>()

    @Suppress("UNCHECKED_CAST")
    fun request(context: ActionContext): T {
        val result = context.request as? T
        return result ?: Problem(title = "Missing request object").throwException()
    }

    fun response(context: ActionContext, entity: R?, statusCode: Int? = null, headers: Map<String, String>? = null): ActionContext {
        context.response.entity = entity
        if (statusCode != null) {
            context.response.statusCode = statusCode
        }
        if (headers != null) {
            context.response.headers.putAll(headers)
        }
        return context
    }

    override fun main(context: ActionContext) {
        val result = main(context, request(context))
        if (result != null) {
            response(context, result)
        }
    }

    open fun main(context: ActionContext, req: T): R? {
        return null
    }

}