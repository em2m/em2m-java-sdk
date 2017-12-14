package io.em2m.policy.model

interface PolicyEngine {
    fun isActionAllowed(actionName: String, context: PolicyContext): Boolean
    fun findAllowedActions(context: PolicyContext): List<String>
}