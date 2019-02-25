package io.em2m.actions.model

import rx.Observable

abstract class ActionTransformerSupport(override val priority: Int) : ActionTransformer {

    open fun doOnNext(context: ActionContext) {
    }

    override fun call(obs: Observable<ActionContext>): Observable<ActionContext> {
        return obs.doOnNext { value ->
            doOnNext(value)
        }
    }
}
