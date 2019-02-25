package io.em2m.flows

import rx.Observable


abstract class TransformerSupport<T>(override val priority: Int) : Transformer<T> {

    open fun doOnNext(value: T) {
    }

    override fun call(obs: Observable<T>): Observable<T> {
        return obs.doOnNext { value ->
            doOnNext(value)
        }
    }
}