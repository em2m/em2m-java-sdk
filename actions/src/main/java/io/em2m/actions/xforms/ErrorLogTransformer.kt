package io.em2m.actions.xforms

import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformerSupport
import io.em2m.actions.model.Priorities
import io.em2m.problem.Problem
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ErrorLogTransformer(private val mapper: ObjectMapper)
    : ActionTransformerSupport(Priorities.ERROR) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun doOnNext(ctx: ActionContext) {
        val actionName = ctx.actionName
        val resource = ctx.resource
        val problem = ctx.response.entity as Problem
        val request = ctx.request
        val stackTrace = problem.extensions["stackTrace"]
        val claims = ctx.claims
        val requestId = ctx.requestId
        val environment = ctx.environment
        val errorMsg = mapOf(
                "problem" to problem,
                "claims" to claims,
                "resource" to resource,
                "stackTrace" to stackTrace,
                "actionName" to actionName,
                "request" to request,
                "requestId" to requestId,
                "environment" to environment)
        logger.error(mapper.writeValueAsString(errorMsg))
    }
}