package io.em2m.actions.xforms


import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Priorities
import io.em2m.policy.model.Environment
import io.em2m.policy.model.PolicyEngine
import io.em2m.problem.Problem
import org.slf4j.LoggerFactory

class AuthorizationCheckTransformer(val policyEngine: PolicyEngine, override val priority: Int = Priorities.AUTHORIZE) : ActionTransformer {

    val log = LoggerFactory.getLogger(javaClass)

    override fun doOnNext(ctx: ActionContext) {
        val actionName = ctx.actionName
        val requestContext = ctx.toPolicyContext()
        val check = policyEngine.checkAction(actionName, requestContext)
        ctx.actionCheck = check

        log.debug("$actionName allowed: ${check.allowed}")

        if (!check.allowed) {
            val status = if (Environment(ctx.environment).Token == null) Problem.Status.NOT_AUTHORIZED else Problem.Status.FORBIDDEN
            val title = if (status == Problem.Status.NOT_AUTHORIZED) "Not Authorized" else "Forbidden"
            if (ctx.debug) {
                Problem(status = status, title = title)
                        .setAny("actionCheck", check)
                        .setAny("claims", ctx.claims)
                        .throwException()
            } else {
                Problem(status = status, title = title).throwException()
            }
        }
    }

}
