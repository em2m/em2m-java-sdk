package io.em2m.flows

import rx.Observable

interface Processor<T> {

    fun process(key: String, value: T): Observable<T>
    fun process(key: String, obs: Observable<T>): Observable<T>
    fun handleError(key: String, value: T): Observable<T>
    fun handleError(key: String, obs: Observable<T>): Observable<T>
    fun transformer(key: String): Observable.Transformer<T, T>
    fun errorTransformer(key: String): Observable.Transformer<T, T>

}