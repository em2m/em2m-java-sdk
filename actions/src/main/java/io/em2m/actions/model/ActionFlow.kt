package io.em2m.actions.model

interface ActionFlow {

    val transformers: List<ActionTransformer>

    fun main(context: ActionContext) {
    }

}