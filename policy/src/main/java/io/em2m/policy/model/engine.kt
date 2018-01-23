package io.em2m.policy.model

data class ActionCheck(val allowed: Boolean, val nStatements: Int, val nAllow: Int, val nDeny: Int)

interface PolicyEngine {
    fun isActionAllowed(actionName: String, context: PolicyContext): Boolean
    fun findAllowedActions(context: PolicyContext): List<String>
    fun checkAction(actionName: String, context: PolicyContext): ActionCheck
}