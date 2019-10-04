package io.em2m.actions.xforms


import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Problem
import io.em2m.flows.Priorities
import io.em2m.policy.model.Environment
import io.em2m.policy.model.PolicyEngine
import org.slf4j.LoggerFactory

class AuthorizationCheckTransformer(val policyEngine: PolicyEngine, override val priority: Int = Priorities.AUTHORIZE) : ActionTransformer {

    val log = LoggerFactory.getLogger(javaClass)

    override fun doOnNext(context: ActionContext) {
        val actionName = context.actionName
        val requestContext = context.toPolicyContext()
        val check = policyEngine.checkAction(actionName, requestContext)

        log.debug("$actionName allowed: ${check.allowed}")

        if (!check.allowed) {
            val status = if (Environment(context.environment).Token == null) Problem.Status.NOT_AUTHORIZED else Problem.Status.FORBIDDEN
            val title = if (status == Problem.Status.NOT_AUTHORIZED) "Not Authorized" else "Forbidden"
            if (context.debug) {
                Problem(status = status, title = title)
                        .setAny("actionCheck", check)
                        .setAny("claims", context.claims)
                        .throwException()
            } else {
                Problem(status = status, title = title).throwException()
            }
        }
    }

}