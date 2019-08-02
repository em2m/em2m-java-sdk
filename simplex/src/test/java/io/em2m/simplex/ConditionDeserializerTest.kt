package io.em2m.simplex

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConditionExpr
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import io.em2m.simplex.parser.SimplexModule
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ConditionDeserializerTest {

    val simplex = Simplex()
            .keys(BasicKeyResolver()
                    .key(Key("ns", "key1"), ConstKeyHandler("value1"))
                    .key(Key("ns", "key2"), ConstKeyHandler("value2"))
                    .key(Key("ns", "key3"), ConstKeyHandler("value3"))
                    .key(Key("ns", "key4"), ConstKeyHandler("value4a"))
            )

    data class ConditionHolder(
            val condition: ConditionExpr
    )

    @Test
    fun parseConditions() {
        val holder: ConditionHolder = jacksonObjectMapper().registerModule(SimplexModule(simplex)).readValue(File("src/test/resources/conditions.json"))
        assertNotNull(holder)
        val result = holder.condition.call(emptyMap())
        assertTrue(result)
    }

    @Test
    fun parseConditions2() {
        val holder: ConditionHolder = jacksonObjectMapper().registerModule(SimplexModule(simplex)).readValue(File("src/test/resources/conditions2.json"))
        assertNotNull(holder)
        val result = holder.condition.call(emptyMap())
        assertTrue(result)
    }

}