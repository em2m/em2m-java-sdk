package io.em2m.flows

import rx.Observable
import rx.Observable.just

@Suppress("unused")
open class BasicProcessor<T>(private val flowResolver: FlowResolver<T>, private val standardXforms: List<Transformer<T>> = emptyList()) : Processor<T> {


    override fun process(value: T): Observable<T> {
        return just(value).compose(transformer(value))
    }

    override fun handleError(value: T): Observable<T> {
        return just(value).compose(errorTransformer(value))
    }

    override fun transformer(value: T): Observable.Transformer<T, T> {

        val flow = flowResolver.findFlow(value) ?: throw FlowNotFound("")

        val transformers = flow.transformers
                .plus(standardXforms)
                .filter { it.priority < Priorities.ERROR }
                .plus(MainTransformer(flow))
                .plus(InitTransformer(flow))
                .sortedBy { it.priority }

        return Observable.Transformer { observable ->
            transformers.fold(observable) { obs, xform -> obs.compose(xform) }
        }
    }

    override fun errorTransformer(value: T): Observable.Transformer<T, T> {

        val flow = flowResolver.findFlow(value) ?: throw FlowNotFound("")

        val transformers = flow.transformers
                .plus(standardXforms)
                .filter { it.priority >= Priorities.ERROR }
                .sortedBy { it.priority }

        return Observable.Transformer { observable ->
            transformers.fold(observable) { obs, xform -> obs.compose(xform) }
        }
    }

    class InitTransformer<T>(val flow: Flow<T>) : Transformer<T> {

        override val priority: Int = Priorities.INIT

        override fun call(obs: Observable<T>): Observable<T> {
            return obs.doOnNext { context ->
                if (context is FlowAware) {
                    context.flow = flow
                }
            }
        }
    }

    class MainTransformer<T>(val flow: Flow<T>) : Transformer<T> {

        override val priority: Int = Priorities.MAIN

        override fun call(obs: Observable<T>): Observable<T> {
            return flow.main(obs)
        }
    }

}