package io.em2m.flows

interface Flow<T> {

    val transformers: List<Transformer<T>>

}
