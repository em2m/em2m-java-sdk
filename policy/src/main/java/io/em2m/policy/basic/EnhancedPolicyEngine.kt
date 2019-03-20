package io.em2m.policy.basic

import io.em2m.policy.model.*
import io.em2m.simplex.Simplex
import java.util.regex.Matcher

class EnhancedPolicyEngine(policySource: PolicySource, val simplex: Simplex = Simplex()) : PolicyEngine {

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
        return findAllowedActions(context).contains(actionName)
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

        if (contextResource == null || contextResource.isNullOrBlank()) return resources.isEmpty() || resources.contains("*")

        resources.forEach { resource ->
            val value = simplex.eval(resource, context.map) as String
            val regex = parseWildcard(value)
            if (regex.matches(contextResource)) return true
        }

        return false
    }

    private fun statementsForRoles(roleIds: List<String>): List<Statement> {
        val roles = roleIds.flatMap { expandRole(it) }.distinct().mapNotNull { roles[it] }
        return roles.flatMap { policiesForRole(it.id) }.distinct()
                .flatMap { it.statements }
                .plus(roles.flatMap { it.statements })
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