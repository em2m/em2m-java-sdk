package io.em2m.actions.xforms


import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Priorities

class RewriteTransformer(override val priority: Int = Priorities.REWRITE) : ActionTransformer {

    override fun doOnNext(ctx: ActionContext) {
        val requestContext = ctx.toPolicyContext().map
        val rewrites = ctx.actionCheck?.rewrites ?: emptyList()
        val result = HashMap<String, Any?>()
        rewrites.forEach { rewrite ->
            try {
                if (rewrite.condition.call(requestContext)) {
                    val key = rewrite.key
                    val value = rewrite.value.call(requestContext)
                    val replace = rewrite.replace
                    if (replace) {
                        result[key] = value
                    } else {
                        val current = result[key]
                        when {
                            current is List<*> -> {
                                result[key] = current.plus(value)
                            }
                            current != null -> {
                                result[key] = listOf(current, value)
                            }
                            else -> {
                                result[key] = value
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
            }
        }
        ctx.rewrites = result
        ctx.scope.putAll(ctx.rewrites)
    }

}
