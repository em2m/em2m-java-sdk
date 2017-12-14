package io.em2m.flows

import java.util.*

@Suppress("LeakingThis")
abstract class MainFlow<T>(transformers: List<Transformer<T>> = ArrayList())
    : Flow<T>, TransformerSupport<T>(Priorities.MAIN) {

    override val transformers: MutableList<Transformer<T>> = transformers.plus(this).toMutableList()

}
