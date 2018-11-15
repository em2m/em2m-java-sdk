package io.em2m.actions.xforms

import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformerSupport
import io.em2m.actions.model.Problem
import io.em2m.flows.Priorities
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rx.Observable

class ErrorLogTransformer(private val mapper: ObjectMapper)
    : ActionTransformerSupport(Priorities.ERROR) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun call(obs: Observable<ActionContext>): Observable<ActionContext> {
        return obs.doOnNext { context ->
            val actionName = context.actionName
            val resource = context.resource
            val problem = context.response.entity as Problem
            val request = context.request
            val stackTrace = problem.extensions["stackTrace"]
            val claims = context.claims
            val requestId = context.requestId
            val environment = context.environment
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
}