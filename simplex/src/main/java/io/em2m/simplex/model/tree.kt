package io.em2m.simplex.model

interface TreeExpr : Expr

class ArrayExpr(val values: List<Expr>) : TreeExpr {

    override fun call(context: ExprContext): Array<Any?> {
        return values.map { it.call(context) }.toTypedArray()
    }

}

class ObjectExpr(val values: Map<String, Expr>) : TreeExpr {

    override fun call(context: ExprContext): Map<String, Any?> {
        return values.mapValues { it.value.call(context) }
    }

}