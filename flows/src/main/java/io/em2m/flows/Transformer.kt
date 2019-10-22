package io.em2m.flows

import rx.Observable


interface Transformer<T> : Observable.Transformer<T, T> {

    val priority: Int

    fun doOnNext(ctx: T)

    companion object {

        fun <T> addPriority(transformer: Transformer<T>, priority: Int): Transformer<T> {

            return object : Transformer<T> {

                override fun doOnNext(ctx: T) {
                    transformer.doOnNext(ctx)
                }

                override fun call(obs: Observable<T>): Observable<T> {
                    return transformer.call(obs)
                }

                override val priority: Int = priority
            }
        }
    }

}
