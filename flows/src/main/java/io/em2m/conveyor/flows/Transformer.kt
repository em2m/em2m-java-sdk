package io.em2m.conveyor.flows

import rx.Observable


interface Transformer<T> : Observable.Transformer<T, T> {

    val priority: Int

    companion object {

        fun <T> addPriority(transformer: Observable.Transformer<T, T>, priority: Int): Transformer<T> {

            return object : Transformer<T> {

                override fun call(obs: Observable<T>): Observable<T> {
                    return transformer.call(obs)
                }

                override val priority: Int = priority
            }
        }
    }

}
