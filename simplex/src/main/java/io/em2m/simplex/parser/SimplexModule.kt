package io.em2m.simplex.parser

import com.fasterxml.jackson.databind.module.SimpleModule
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.ConditionExpr
import io.em2m.simplex.model.Expr


class SimplexModule(simplex: Simplex = Simplex()) : SimpleModule() {

    init {
        addDeserializer(Expr::class.java, TreeDeserializer(simplex));
        addDeserializer(ConditionExpr::class.java, ConditionsDeserializer(simplex))
    }

}