package io.em2m.actions.xforms

import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import org.slf4j.Logger

class LoggingTransformer(val log: Logger, val lazyMessage: (context: ActionContext) -> Any, override val priority: Int) : ActionTransformer {

    override fun doOnNext(ctx: ActionContext) {
        log.debug(lazyMessage(ctx).toString())
    }
}
