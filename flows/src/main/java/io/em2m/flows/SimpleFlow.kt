package io.em2m.flows

import rx.Observable

class SimpleFlow<T>(val fn: (Observable<T>) -> Observable<T>) : FlowSupport<T>() {

    override fun main(obs: Observable<T>): Observable<T> {
        return fn.invoke(obs)
    }

}
