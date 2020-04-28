package io.em2m.actions.model

import io.em2m.problem.Problem
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class TypedActionFlow<T, R>(val requestType: Class<out Any>, val resultType: Class<out Any>) : ActionFlow {

    override val transformers = ArrayList<ActionTransformer>()
    open val log: Logger = LoggerFactory.getLogger(javaClass)

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