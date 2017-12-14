package io.em2m.flows


interface FlowResolver<T> {

    fun findFlow(key: String): Flow<T>?

}