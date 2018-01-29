package io.em2m.flows

import rx.Observable

interface Flow<T> {

    val transformers: List<Transformer<T>>

    fun main(obs: Observable<T>): Observable<T> {
        return obs
    }

}
