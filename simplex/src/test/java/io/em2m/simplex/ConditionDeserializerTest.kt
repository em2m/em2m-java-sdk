package io.em2m.simplex

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.simplex.model.Condition
import io.em2m.simplex.parser.ConditionsDeserializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File


class ConditionDeserializerTest {

    data class ConditionHolder(
            @JsonDeserialize(using = ConditionsDeserializer::class)
            val conditions: List<Condition>
    )

    @Test
    fun parseConditions() {
        val holder: ConditionHolder = jacksonObjectMapper().readValue(File("src/test/resources/conditions.json"))
        assertNotNull(holder)
        assertEquals(4, holder.conditions.size)
    }

}