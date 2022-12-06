package io.em2m.actions.model

class OptionalActionTransformer(private val transformer: ActionTransformer) : ActionTransformer by transformer {
    override fun doOnNext(ctx: ActionContext) {
        val flow = ctx.flow
        val flowOptIns = flow?.getTransformerOptIns() ?: emptyList()
        if (flowOptIns.contains(transformer::class)) {
            transformer.doOnNext(ctx)
        }
    }

    private fun ActionFlow.getTransformerOptIns() = this::class
        .annotations
        .filterIsInstance<TransformerOptIn>()
        .flatMap { it.transformerOptIns.toList() }
}
