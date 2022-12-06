package io.em2m.actions.xforms

import io.em2m.actions.model.*
import io.em2m.simplex.evalPath

abstract class AuditingTransformer: ActionTransformerSupport(Priorities.AUDIT) {
    abstract val defaultAuditPaths: Set<String>
    override fun doOnNext(ctx: ActionContext) {
        val flow = ctx.flow
        val auditPaths = flow?.getAuditPaths() ?: emptySet()
        val auditValues = auditPaths.associateWith { ctx.evalPath(it) }
        if (auditPaths.isNotEmpty()) {
            saveAudit(auditValues)
        }
    }

    abstract fun saveAudit(auditValues: Map<String, Any?>)

    private fun ActionFlow.getAuditPaths(): Set<String> {
        val flowAuditAnnotation = this::class.java
            .annotations
            .filterIsInstance(Audit::class.java)
            .firstOrNull()
        return when(flowAuditAnnotation) {
            null -> emptySet()
            else -> flowAuditAnnotation.contextAuditPaths.toSet().plus(defaultAuditPaths)
        }
    }
}
