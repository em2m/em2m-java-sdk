package io.em2m.conveyor.flows

import rx.Observable

class SimpleFlow<T>(val fn: (Observable<T>) -> Observable<T>) : MainFlow<T>() {

    override fun call(obs: Observable<T>): Observable<T> {
        return fn.invoke(obs)
    }

}
