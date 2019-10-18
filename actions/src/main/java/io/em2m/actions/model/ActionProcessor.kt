package io.em2m.actions.model

interface ActionProcessor {
    fun process(context: ActionContext)
    fun handleError(context: ActionContext)
}