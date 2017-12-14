package io.em2m.search.core.expr

import io.em2m.search.core.model.Bucket
import io.em2m.search.core.model.BucketContext
import io.em2m.search.core.model.RowContext
import io.em2m.search.core.model.SearchRequest
import io.em2m.simplex.basic.BasicKeyResolver
import io.em2m.simplex.basic.BasicPipeTransformResolver
import io.em2m.simplex.model.Key
import io.em2m.simplex.parser.ExprParser
import org.junit.Assert
import org.junit.Test


class FieldExprTest : Assert() {

    val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("bucket", "key") to BucketKeyKeyHandler(),
            Key("field", "*") to FieldKeyHandler()))

    val pipeResolver = BasicPipeTransformResolver(mapOf("upperCase" to UpperCasePipe(), "capitalize" to CapitalizePipe()))

    val request = SearchRequest()

    val parser = ExprParser(keyResolver, pipeResolver)

    @Test
    fun testParse() {
        val exprStr = "#{ns:key1 | upperCase}/#{ns:key2 | capitalize}".replace("#", "$")
        val expr = parser.parse(exprStr)
        assertNotNull(expr)
        val result = expr.call(RowContext(mapOf("request" to request, "scope" to emptyMap<String, Any?>())).map)
        assertNotNull(result)
        assertEquals("VALUE1/Value2", result)
    }

    @Test
    fun testFieldNames() {
        val exprStr = "Label: #{field:fieldName | capitalize}".replace("#", "$")
        val expr = parser.parse(exprStr)
        assertEquals(listOf("fieldName"), expr.fields)
        assertNotNull(expr)
        val result = expr.call(RowContext(
                mapOf(
                        "fieldValues" to mapOf("fieldName" to "fieldValue"),
                        "request" to request,
                        "scope" to emptyMap<String, Any?>())).map)
        assertNotNull(result)
        assertEquals("Label: FieldValue", result)
    }

    @Test
    fun testBucketLabel() {
        val exprStr = "#{bucket:key | capitalize}".replace("#", "$")
        val expr = parser.parse(exprStr)
        assertEquals(emptyList<String>(), expr.fields)
        assertNotNull(expr)
        val bucket = Bucket(key = "ford", count = 5)
        val result = expr.call(BucketContext(request, emptyMap(), bucket).map)
        assertNotNull(result)
        assertEquals("Ford", result)
    }

}

