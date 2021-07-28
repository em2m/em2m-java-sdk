package io.em2m.actions.model

import io.em2m.problem.Problem.Companion.notFound

open class BasicActionProcessor(
    private val flowResolver: ActionFlowResolver,
    private val standardXforms: List<ActionTransformer> = emptyList()
) : ActionProcessor {

    override fun process(context: ActionContext) {
        val flow = flowResolver.findFlow(context) ?: notFound({ "Flow not found: ${context.actionName}" })
        context.flow = flow
        val transformers = flow.transformers
            .plus(standardXforms)
            .filter { it.priority < Priorities.ERROR }
            .plus(MainTransformer(flow))
            .sortedBy { it.priority }
        transformers.forEach { xform -> xform.doOnNext(context) }
    }

    override fun handleError(context: ActionContext) {
        val flow = flowResolver.findFlow(context)
        val transformers = (flow?.transformers ?: emptyList())
            .plus(standardXforms)
            .filter { it.priority >= Priorities.ERROR }
            .sortedBy { it.priority }
        transformers.forEach { xform -> xform.doOnNext(context) }
    }

    class MainTransformer(private val flow: ActionFlow) : ActionTransformerSupport(Priorities.MAIN) {

        override fun doOnNext(ctx: ActionContext) {
            flow.main(ctx)
        }
    }

}
