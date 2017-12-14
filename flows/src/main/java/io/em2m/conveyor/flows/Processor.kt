package io.em2m.conveyor.flows

import rx.Observable
import rx.Subscription

interface Processor<T> {

    fun process(key: String, value: T): Observable<T>
    fun process(key: String, obs: Observable<T>): Observable<T>
    fun transformer(key: String): Observable.Transformer<T, T>

}