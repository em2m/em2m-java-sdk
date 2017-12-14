package io.em2m.conveyor.actions.xforms

import io.em2m.conveyor.actions.model.ActionContext
import io.em2m.conveyor.flows.Transformer
import org.slf4j.Logger
import rx.Observable

class LoggingTransformer(val log: Logger, val lazyMessage: (context: ActionContext) -> Any, override val priority: Int) : Transformer<ActionContext> {

    override fun call(source: Observable<ActionContext>): Observable<ActionContext> {
        return source.doOnNext {
            log.debug(lazyMessage(it).toString())
        }
    }
}
