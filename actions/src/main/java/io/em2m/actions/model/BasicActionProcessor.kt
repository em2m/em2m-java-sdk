package io.em2m.actions.model

open class BasicActionProcessor(private val flowResolver: ActionFlowResolver, private val standardXforms: List<ActionTransformer> = emptyList()) : ActionProcessor {

    override fun process(context: ActionContext) {
        val flow = flowResolver.findFlow(context) ?: throw FlowNotFound("")
        context.flow = flow
        val transformers = flow.transformers
                .asSequence()
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