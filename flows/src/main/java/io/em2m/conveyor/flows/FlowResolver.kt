package io.em2m.conveyor.flows


interface FlowResolver<T> {

    fun findFlow(key: String): Flow<T>?

}