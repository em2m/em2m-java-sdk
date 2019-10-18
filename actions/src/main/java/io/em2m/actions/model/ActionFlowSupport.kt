package io.em2m.actions.model

open class ActionFlowSupport(transformers: List<ActionTransformer>) : ActionFlow {

    final override val transformers: MutableList<ActionTransformer> = ArrayList()

    init {
        this.transformers.addAll(transformers)
    }

}