package io.em2m.actions.xforms


import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionError
import io.em2m.actions.model.ActionTransformer
import io.em2m.flows.Priorities
import io.em2m.policy.model.PolicyEngine
import org.slf4j.LoggerFactory
import rx.Observable

class AuthorizationCheckTransformer(val policyEngine: PolicyEngine, override val priority: Int = Priorities.AUTHORIZE) : ActionTransformer {

    val log = LoggerFactory.getLogger(javaClass)

    override fun call(single: Observable<ActionContext>): rx.Observable<ActionContext> {
        return single.doOnNext { context ->

            val actionName = context.actionName
            val requestContext = context.toPolicyContext()
            val allowed = policyEngine.isActionAllowed(actionName, requestContext)

            log.debug("$actionName allowed: $allowed")

            if (!allowed) {
                throw ActionError(ActionError.Status.FORBIDDEN, messages = listOf(ActionError.Message("Forbidden")))
            }
        }
    }

}