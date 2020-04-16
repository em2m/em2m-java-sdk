package io.em2m.search.core.expr

import io.em2m.search.core.model.Bucket
import io.em2m.search.core.model.BucketContext
import io.em2m.search.core.model.RowContext
import io.em2m.search.core.model.SearchRequest
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.BasicPipeTransformResolver
import io.em2m.simplex.model.Key
import io.em2m.simplex.parser.ExprParser
import io.em2m.simplex.std.Arrays
import io.em2m.simplex.std.Numbers
import io.em2m.simplex.std.Strings
import org.junit.Assert
import org.junit.Test


class FieldExprTest : Assert() {

    private val keyResolver = BasicKeyResolver(mapOf(
            Key("ns", "key1") to ConstKeyHandler("value1"),
            Key("ns", "key2") to ConstKeyHandler("value2"),
            Key("bucket", "key") to BucketKeyKeyHandler(),
            Key("field", "*") to FieldKeyHandler()))

    private val pipeResolver = BasicPipeTransformResolver()
            .delegate(Numbers.pipes)
            .delegate(Strings.pipes)
            .delegate(Arrays.pipes)

    private val request = SearchRequest()

    private val parser = ExprParser(keyResolver, pipeResolver)

    @Test
    fun testParse() {
        val exprStr = "#{ns:key1 | upperCase}/#{ns:key2 | capitalize}".replace("#", "$")
        val expr = parser.parse(exprStr)
        assertNotNull(expr)
        val result = expr.call(RowContext(request, emptyMap(), emptyMap()).toMap())
        assertNotNull(result)
        assertEquals("VALUE1/Value2", result)
    }

    @Test
    fun testFieldNames() {
        val exprStr = "Label: #{field:fieldName | capitalize}".replace("#", "$")
        val expr = parser.parse(exprStr)
        val fields = FieldKeyHandler.fields(expr)
        assertEquals(listOf("fieldName"), fields)
        assertNotNull(expr)
        val result = expr.call(RowContext(
                mapOf(
                        "fieldValues" to mapOf("fieldName" to "fieldValue"),
                        "request" to request,
                        "scope" to emptyMap<String, Any?>())).toMap())
        assertNotNull(result)
        assertEquals("Label: FieldValue", result)
    }

    @Test
    fun testBucketLabel() {
        val exprStr = "#{bucket:key | capitalize}".replace("#", "$")
        val expr = parser.parse(exprStr)
        assertNotNull(expr)
        val bucket = Bucket(key = "ford", count = 5)
        val result = expr.call(BucketContext(request, emptyMap(), bucket).toMap())
        assertNotNull(result)
        assertEquals("Ford", result)
    }

    @Test
    fun testMultipleFields() {
        val exprStr = "\${field:key1,key2 | notNull | join:, }"
        val expr = parser.parse(exprStr)
        val fields = FieldKeyHandler.fields(expr)
        assertEquals(listOf("key1", "key2"), fields)
        assertNotNull(expr)
        val result = expr.call(RowContext(
                mapOf(
                        "fieldValues" to mapOf("key1" to "value1", "key2" to "value2"),
                        "request" to request,
                        "scope" to emptyMap<String, Any?>())).toMap())
        assertNotNull(result)
        assertEquals("value1, value2", result)
    }

}

