package io.em2m.actions.model

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class Audit(vararg val contextAuditPaths: String)
