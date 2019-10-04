package io.em2m.actions.model

import io.em2m.flows.Transformer
import rx.Observable

interface ActionTransformer : Transformer<ActionContext> {

    override fun call(obs: Observable<ActionContext>): Observable<ActionContext> {
        return obs.doOnNext { ctx ->
            doOnNext(ctx)
        }
    }

}