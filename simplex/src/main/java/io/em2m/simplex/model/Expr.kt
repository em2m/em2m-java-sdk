package io.em2m.simplex.model

interface Expr {
    fun call(context: ExprContext): Any?
}