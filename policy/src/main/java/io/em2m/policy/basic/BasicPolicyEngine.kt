package io.em2m.policy.basic

import io.em2m.policy.model.*
import io.em2m.simplex.Simplex
import org.slf4j.LoggerFactory
import java.util.regex.Matcher

class BasicPolicyEngine(policySource: PolicySource, val simplex: Simplex = Simplex()) : PolicyEngine {

    private var logger = LoggerFactory.getLogger(javaClass)
    private val roles = policySource.roles.associateBy { it.id }
    private val policies = policySource.policies.associateBy { it.id }

    override fun findAllowedActions(context: PolicyContext): List<String> {
        val roles = context.claims.roles.plus("anonymous").distinct()
        val statements = statementsForRoles(roles)
            .filter { testResource(it, context) }
            .filter { it.condition.call(context.map) }
            .filter { it.effect == Effect.Allow }
        return statements.flatMap { it.actions }.distinct()
    }

    override fun isActionAllowed(actionName: String, context: PolicyContext): Boolean {
        return checkAction(actionName, context).allowed
    }

    override fun checkAction(actionName: String, context: PolicyContext): ActionCheck {
        val roles = context.claims.roles.plus("anonymous").distinct()
        val statements = statementsForRolesAndAction(roles, actionName).filter { testResource(it, context) }
        val matches = statements.filter {
            try {
                it.condition.call(context.map)
            } catch (ex: Exception) {
                false
            }
        }
        val nDeny = matches.count { it.effect == Effect.Deny }
        val nAllow = matches.count { it.effect == Effect.Allow }
        val rewrites = matches.flatMap { it.scope }
        val allowed = if (nDeny > 0) {
            logger.warn("User attempted to execute an explicitly denied action: Account ID = ${context.claims.sub}, Action = $actionName")
            false
        } else nAllow > 0
        return ActionCheck(allowed, statements.size, nAllow, nDeny, rewrites)
    }

    private fun expandRole(roleId: String): List<String> {
        val role = roles[roleId]
        return if (role != null) {
            listOf(roleId).plus(role.inherits.flatMap { expandRole(it) })
        } else listOf(roleId)
    }

    private fun testResource(statement: Statement, context: PolicyContext): Boolean {

        val resources = statement.resource
        val contextResource = context.resource

        if (contextResource == null || contextResource.isBlank()) return resources.isEmpty() || resources.contains(
            "*"
        )

        resources.forEach { resource ->
            val value = simplex.eval(resource, context.map) as String
            val regex = parseWildcard(value)
            if (regex.matches(contextResource)) return true
        }

        return false
    }

    private fun statementsForRolesAndAction(roles: List<String>, actionName: String): List<Statement> {
        return statementsForRoles(roles).filter { statement -> matchesAction(statement, actionName) }
    }

    private fun statementsForRoles(roleIds: List<String>): List<Statement> {
        val roles = roleIds.flatMap { expandRole(it) }.distinct().mapNotNull { roles[it] }
        return roles.flatMap { policiesForRole(it.id) }.distinct()
            .flatMap { it.statements }
            .plus(roles.flatMap { it.statements })
    }

    private fun matchesAction(statement: Statement, actionName: String): Boolean {
        return statement.actions.find {
            val pattern = it.replace("*", "(.*)")
            actionName.matches(Regex(pattern))
        } != null
    }

    private fun policiesForRole(role: String): List<Policy> {
        return roles[role]?.policies?.mapNotNull { policyId -> policies[policyId] } ?: emptyList()
    }

    private fun parseWildcard(text: String): Regex {
        var value = text.replace("?", "_QUESTION_MARK_").replace("*", "_STAR_")
        value = Matcher.quoteReplacement(value)
        value = value.replace("_QUESTION_MARK_", ".?").replace("_STAR_", ".*")
        return Regex("^" + Matcher.quoteReplacement(value) + "$")
    }

}
