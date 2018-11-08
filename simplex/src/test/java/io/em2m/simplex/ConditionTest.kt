package io.em2m.sdk.simplex

import io.em2m.simplex.std.*
import org.junit.Assert
import org.junit.Test
import java.util.*

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

    @Test
    fun anyStringLikeAllowed() {
        val keyVals = listOf("foo.bar", "foo.baz")
        val conditionVals = listOf("foo.*")
        val condition = ForAnyStringLike()
        assert(condition.test(keyVals, conditionVals))
    }

    @Test
    fun bool() {
        val condition = Bool()
        assertTrue(condition.test(true, true))
        assertFalse(condition.test(true, false))
        assertTrue(condition.test("true", true))
        assertTrue(condition.test("true", "true"))
    }

    @Test
    fun dateEquals() {
        val condition = DateEquals()
        val date1 = Date()
        val date2 = date1.clone()
        assert(condition.test(date1, date2))
    }

    @Test
    fun dateEqualsString() {
        val condition = DateEquals()
        val date1 = "2015-02-20"
        val date2 = "2015-02-20"
        assert(condition.test(date1, date2))
    }

    @Test
    fun dateEqualsFailure() {
        val condition = DateEquals()
        val date1 = "2015-02-21"
        val date2 = "2015-02-20"
        assertFalse(condition.test(date1, date2))
    }

    @Test
    fun testNegativeDates() {
        val condition = DateEquals()
        val date1 = "-112303030303"
        val date2 = "-112303030303"
        assert(condition.test(date1, date2))
    }

    @Test
    fun dateNotEquals() {
        val condition = DateNotEquals()
        val date1 = "2015-04-20"
        val date2 = "2015-05-20"
        assert(condition.test(date1, date2))
    }

    @Test
    fun dateLessThan() {
        val condition = DateLessThan()
        val date1 = "2015-04-20"
        val date2 = "2015-05-20"
        assert(condition.test(date1, date2))
    }

    @Test
    fun dateGreaterThan() {
        val condition = DateGreaterThan()
        val date1 = "2015-04-20"
        val date2 = "2015-05-20"
        assert(condition.test(date2, date1))
    }

    @Test
    fun dateGreaterThanEquals() {
        val condition = DateGreaterThanEquals()
        val date1 = "2015-04-20"
        val date2 = "2015-04-20"
        assert(condition.test(date2, date1))
    }

    @Test
    fun dateLessThanEquals() {
        val condition = DateLessThanEquals()
        val date1 = "2015-04-20"
        val date2 = "2015-04-20"
        assert(condition.test(date2, date1))
    }

}