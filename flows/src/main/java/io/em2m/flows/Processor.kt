package io.em2m.flows

import rx.Observable

interface Processor<T> {

    fun process(value: T): Observable<T>
    fun handleError(value: T): Observable<T>
    fun transformer(value: T): Observable.Transformer<T, T>
    fun errorTransformer(value: T): Observable.Transformer<T, T>

}