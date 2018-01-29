package io.em2m.actions.model

import io.em2m.flows.FlowSupport

open class ActionFlowSupport(transformers: List<ActionTransformer>) : ActionFlow, FlowSupport<ActionContext>(transformers) {

    constructor(vararg transformers: ActionTransformer) : this(transformers.toList())
}