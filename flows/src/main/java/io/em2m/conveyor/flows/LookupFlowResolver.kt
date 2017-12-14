package io.em2m.conveyor.flows

import com.google.inject.Injector
import kotlin.reflect.KClass


open class LookupFlowResolver<T>(val injector: Injector, val classes: Map<String, KClass<out Flow<T>>> = emptyMap(), val instances: Map<String, Flow<T>> = emptyMap()) : FlowResolver<T> {

    override fun findFlow(key: String): Flow<T>? {
        val flowClass = classes[key]?.java
        val flowInstance = instances[key]
        return if (flowClass != null) {
            injector.getInstance(flowClass)
        } else flowInstance
    }

}