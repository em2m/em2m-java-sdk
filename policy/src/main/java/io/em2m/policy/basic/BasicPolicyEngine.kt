package io.em2m.policy.basic

import io.em2m.policy.model.*
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.ConditionResolver
import io.em2m.simplex.model.KeyResolver
import io.em2m.simplex.model.PipeTransformResolver
import org.slf4j.LoggerFactory
import java.util.regex.Matcher

class BasicPolicyEngine(
        val policySource: PolicySource,
        keys: KeyResolver? = null,
        conditions: ConditionResolver? = null,
        pipes: PipeTransformResolver? = null) : PolicyEngine {

    private var LOG = LoggerFactory.getLogger(javaClass)

    private val expr = Simplex()

    init {
        if (keys != null) expr.keys(keys)
        if (conditions != null) expr.conditions(conditions)
        if (pipes != null) expr.pipes(pipes)
    }

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

    private fun testResource(statement: Statement, context: PolicyContext): Boolean {

        val resources = statement.resource
        val contextResource = context.resource

        if (contextResource == null || contextResource.isNullOrBlank()) return resources.isEmpty() || resources.contains("*")

        resources.forEach { resource ->
            val value = expr.eval(resource, context.map) as String
            val regex = parseWildcard(value)
            if (regex.matches(contextResource)) return true
        }

        return false
    }

    private fun statementsForRolesAndAction(roles: List<String>, actionName: String): List<Statement> {
        return statementsForRoles(roles).filter { statement -> matchesAction(statement, actionName) }
    }

    private fun statementsForRoles(roles: List<String>): List<Statement> {
        return roles.flatMap { policiesForRole(it) }.distinct().flatMap { it.statements }
    }

    private fun matchesAction(statement: Statement, actionName: String): Boolean {
        return statement.actions.find {
            val pattern = it.replace("*", "(.*)")
            actionName.matches(Regex(pattern))
        } != null
    }

    private fun policiesForRole(role: String): List<Policy> {
        return policySource.policiesForRole(role)
    }

    private fun parseWildcard(text: String): Regex {
        var value = text.replace("?", "_QUESTION_MARK_").replace("*", "_STAR_")
        value = Matcher.quoteReplacement(value)
        value = value.replace("_QUESTION_MARK_", ".?").replace("_STAR_", ".*")
        return Regex("^" + Matcher.quoteReplacement(value) + "$")
    }

}