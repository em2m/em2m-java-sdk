package io.em2m.conveyor.flows

interface Flow<T> {

    val transformers: List<Transformer<T>>

}
