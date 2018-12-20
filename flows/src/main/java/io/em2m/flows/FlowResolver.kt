package io.em2m.flows


interface FlowResolver<T> {

    fun findFlow(context: T): Flow<T>?

}