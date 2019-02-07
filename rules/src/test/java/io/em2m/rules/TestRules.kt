package io.em2m.rules

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.rules.basic.BasicRuleEngine
import io.em2m.rules.parser.RulesModule
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.*
import io.em2m.simplex.parser.SimplexModule
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class TestRules {

    private val simplex = Simplex()

    init {
        simplex.keys(BasicKeyResolver(mapOf(
                Key("field", "*") to ContextKeyHandler(),
                Key("field", "*") to ContextKeyHandler()
        )))
    }

    private val mapper = jacksonObjectMapper().registerModule(SimplexModule(simplex)).registerModule(RulesModule(simplex))

    @Test
    fun testParse() {
        val rule: Rule = mapper.readValue(File("src/test/resources/frog.json"))
        assertNotNull(rule)
        println(rule)
    }

    @Test
    fun testBasicEngine() {
        val rule: Rule = mapper.readValue(File("src/test/resources/frog.json"))
        val engine = BasicRuleEngine(listOf(rule))
        val keys = BasicKeyResolver(mapOf(Key("var", "*") to VarKeyHandler(engine)))
        val context = RuleContext(mapOf("skill" to listOf("Croaks", "EatsFlies"), "keys" to keys))
        val result = engine.test(context, Assertion("color", listOf("green")))
        assertTrue(result)
    }

    @Test
    fun testTaxes() {
        val rule: Rule = mapper.readValue(File("src/test/resources/tax.json"))
        val engine = BasicRuleEngine(listOf(rule))
        val keys = BasicKeyResolver(mapOf(Key("var", "*") to VarKeyHandler(engine)))
        val context = RuleContext(mapOf("grossSalary" to 70000, "keys" to keys))
        val result = engine.values(context, "taxRate")
        assertEquals(result, listOf(0.2))
    }

    class ContextKeyHandler : KeyHandler {

        override fun call(key: Key, context: ExprContext): Any? {
            return context[key.name]
        }

    }

    class VarKeyHandler(val engine: RuleEngine) : KeyHandler {

        override fun call(key: Key, context: ExprContext): Any? {
            return engine.values(RuleContext(context), key.name)
        }
    }

}