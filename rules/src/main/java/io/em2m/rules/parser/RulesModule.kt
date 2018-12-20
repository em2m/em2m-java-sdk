package io.em2m.rules.parser

import com.fasterxml.jackson.databind.module.SimpleModule
import io.em2m.rules.Assertions
import io.em2m.simplex.Simplex

class RulesModule(simplex: Simplex) : SimpleModule() {

    init {
        addDeserializer(Assertions::class.java, AssertionsDeserializer(simplex))
    }

}
