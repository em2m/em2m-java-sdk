package io.em2m.actions.model

interface ActionFlowResolver {
    fun findFlow(context: ActionContext): ActionFlow?
}