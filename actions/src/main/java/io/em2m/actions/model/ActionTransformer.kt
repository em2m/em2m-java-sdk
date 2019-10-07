package io.em2m.actions.model

interface ActionTransformer {

    val priority: Int

    fun doOnNext(ctx: ActionContext)

}