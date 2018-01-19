package io.em2m.flows

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import rx.Observable
import kotlin.reflect.KClass


class BasicProcessor<T>(val flowResolver: FlowResolver<T>, val standardXforms: List<Transformer<T>> = emptyList()) : Processor<T> {

    override fun process(key: String, value: T): Observable<T> {
        return process(key, Observable.just(value))
    }

    override fun process(key: String, obs: Observable<T>): Observable<T> {
        return obs.compose(transformer(key))
    }

    override fun transformer(key: String): Observable.Transformer<T, T> {

        val flow = flowResolver.findFlow(key) ?: throw FlowNotFound(key)

        val transformers = ArrayList(flow.transformers)
                .plus(standardXforms)
                .sortedBy { it.priority }

        return Observable.Transformer { observable ->
            transformers.fold(observable) { single, xform -> single.compose(xform) }
        }
    }

    class Builder<T> {

        private val classes = HashMap<String, KClass<out Flow<T>>>()
        private val instances = HashMap<String, Flow<T>>()
        private val modules = ArrayList<Module>()
        private var injector: Injector? = null
        private val xforms = ArrayList<Transformer<T>>()

        fun injector(injector: Injector): Builder<T> {
            this.injector = injector
            return this
        }

        fun module(module: Module): Builder<T> {
            modules.add(module)
            return this
        }

        fun transformer(transformer: Transformer<T>): Builder<T> {
            xforms.add(transformer)
            return this
        }

        fun flow(key: String, flow: Flow<T>): Builder<T> {
            instances.put(key, flow)
            return this
        }

        fun flow(key: String, flowClass: KClass<out Flow<T>>): Builder<T> {
            classes.put(key, flowClass)
            return this
        }

        fun build(): Processor<T> {
            val injector = injector?.createChildInjector(modules) ?: Guice.createInjector(modules)
            val resolver = LookupFlowResolver(injector, classes, instances)
            return BasicProcessor(resolver, xforms)
        }

    }

}