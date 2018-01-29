package io.em2m.flows


abstract class FlowSupport<T>(transformers: List<Transformer<T>> = ArrayList()) : Flow<T> {

    final override val transformers: MutableList<Transformer<T>> = ArrayList()

    init {
        this.transformers.addAll(transformers)
    }

}