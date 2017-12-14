package io.em2m.sdk.simplex

import io.em2m.simplex.conditions.ForAllStringEquals
import io.em2m.simplex.conditions.ForAnyStringEquals
import org.junit.Assert
import org.junit.Test

class ConditionTest : Assert() {

    @Test
    fun anyStringEqualsAllowed() {
        val keyVals = listOf("foo", "bar")
        val conditionVals = listOf("foo")
        val condition = ForAnyStringEquals()
        assert(condition.test(keyVals, conditionVals))
    }

    @Test
    fun anyStringEqualsForbidden() {
        val keyVals = listOf("foo", "bar")
        val conditionVals = listOf("baz")
        val condition = ForAnyStringEquals()
        assert(!condition.test(keyVals, conditionVals))
    }

    @Test
    fun allStringEqualsAllowed() {
        val keyVals = listOf("foo")
        val conditionVals = listOf("foo", "bar")
        val condition = ForAllStringEquals()
        assert(condition.test(keyVals, conditionVals))
    }

    @Test
    fun allStringEqualsForbidden() {
        val keyVals = listOf("foo", "bar", "baz")
        val conditionVals = listOf("foo", "bar")
        val condition = ForAllStringEquals()
        assert(!condition.test(keyVals, conditionVals))
    }
}