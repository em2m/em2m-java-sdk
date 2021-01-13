package io.em2m.actions.xforms


import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Priorities

class RewriteTransformer(override val priority: Int = Priorities.REWRITE) : ActionTransformer {

    override fun doOnNext(ctx: ActionContext) {
        val actionName = ctx.actionName
        val requestContext = ctx.toPolicyContext().map
        val rewrites = ctx.actionCheck?.rewrites ?: emptyList()
        ctx.rewrites = rewrites.mapNotNull { rewrite ->
            try {
                if (rewrite.condition.call(requestContext)) {
                    val key = rewrite.key
                    val value = rewrite.value.call(requestContext)
                     key to value
                } else null
            } catch (ex: Exception) {
                null
            }
        }.toMap()
    }

}
