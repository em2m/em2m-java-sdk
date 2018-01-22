package io.em2m.policy.basic

import io.em2m.policy.model.*
import io.em2m.simplex.basic.ExprService
import io.em2m.simplex.model.ConditionResolver
import io.em2m.simplex.model.KeyResolver
import org.slf4j.LoggerFactory
import java.util.regex.Matcher

class BasicPolicyEngine(val policySource: PolicySource, keyResolver: KeyResolver, conditionResolver: ConditionResolver) : PolicyEngine {

    var LOG = LoggerFactory.getLogger(javaClass)

    val expr = ExprService(keyResolver, conditionResolver)

    override fun findAllowedActions(context: PolicyContext): List<String> {
        val roles = context.claims.roles.plus("anonymous").distinct()
        val statements = statementsForRoles(roles)
                .filter { testResource(it, context) }
                .filter { expr.testConditions(it.condition, context.map) }
                .filter { it.effect == Effect.Allow }
        return statements.flatMap { it.actions }.distinct()
    }

    override fun isActionAllowed(actionName: String, context: PolicyContext): Boolean {
        return checkAction(actionName, context).allowed
    }

    override fun checkAction(actionName: String, context: PolicyContext): ActionCheck {
        val roles = context.claims.roles.plus("anonymous").distinct()
        val statements = statementsForRolesAndAction(roles, actionName).filter { testResource(it, context) }
        val matches = statements.filter { expr.testConditions(it.condition, context.map) }
        val nDeny = matches.count { it.effect == Effect.Deny }
        val nAllow = matches.count { it.effect == Effect.Allow }
        val allowed = if (nDeny > 0) {
            LOG.warn("User attempted to execute an explicitly denied action: Account ID = ${context.claims.sub}, Action = $actionName")
            false
        } else nAllow > 0
        return ActionCheck(allowed, statements.size, nAllow, nDeny)
    }

    fun testResource(statement: Statement, context: PolicyContext): Boolean {

        val resources = statement.resource
        val contextResource = context.resource

        if (contextResource == null || contextResource.isNullOrBlank()) return resources.isEmpty() || resources.contains("*")

        resources.forEach { resource ->
            val value = expr.getValue(resource, context.map) as String
            val regex = parseWildcard(value)
            if (regex.matches(contextResource)) return true
        }

        return false
    }


    fun statementsForRolesAndAction(roles: List<String>, actionName: String): List<Statement> {
        val result = statementsForRoles(roles).filter { statement -> matchesAction(statement, actionName) }
        return result
    }

    fun statementsForRoles(roles: List<String>): List<Statement> {
        return roles.flatMap { policiesForRole(it) }.distinct().flatMap { it.statements }
    }

    fun matchesAction(statement: Statement, actionName: String): Boolean {
        return statement.actions.find {
            val pattern = it.replace("*", "(.*)")
            actionName.matches(Regex(pattern))
        } != null
    }

    fun policiesForRole(role: String): List<Policy> {
        return policySource.policiesForRole(role)
    }

    fun parseWildcard(text: String): Regex {
        var value = text.replace("?", "_QUESTION_MARK_").replace("*", "_STAR_")
        value = Matcher.quoteReplacement(value)
        value = value.replace("_QUESTION_MARK_", ".?").replace("_STAR_", ".*")
        return Regex("^" + Matcher.quoteReplacement(value) + "$")
    }

}