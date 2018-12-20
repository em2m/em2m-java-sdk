package io.em2m.flows

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import kotlin.reflect.KClass

abstract class AbstractBuilder<T> {

    protected val modules = ArrayList<Module>()
    protected var injector: Injector? = null
    protected val xforms = ArrayList<Transformer<T>>()

    open fun injector(injector: Injector): AbstractBuilder<T> {
        this.injector = injector
        return this
    }

    open fun module(module: Module): AbstractBuilder<T> {
        modules.add(module)
        return this
    }

    open fun transformer(transformer: Transformer<T>): AbstractBuilder<T> {
        xforms.add(transformer)
        return this
    }

    abstract fun resolver(injector: Injector): FlowResolver<T>

    open fun build(): Processor<T> {
        val injector = injector?.createChildInjector(modules) ?: Guice.createInjector(modules)
        val resolver = resolver(injector)
        return BasicProcessor(resolver, xforms)
    }

}