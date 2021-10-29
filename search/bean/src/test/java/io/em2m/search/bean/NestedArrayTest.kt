package io.em2m.search.bean

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.core.model.Field
import io.em2m.search.core.model.FnIdMapper
import io.em2m.search.core.model.RangeQuery
import io.em2m.search.core.model.SearchRequest
import io.em2m.utils.coerceNonNull
import org.junit.Test
import kotlin.test.assertEquals

class NestedArrayTest {

    val json = jacksonObjectMapper().readTree(
        """
        [
          {"id" : "1", "data": [ { "x": 1}, { "x": -1} ]},
          {"id" : "2", "data": [ { "x": 2}, { "x": -2} ]},
          {"id" : "3", "data": [ { "x": 3}, { "x": -3} ]},
          {"id" : "4", "data": [ { "x": 4}, { "x": -4} ]},
          {"id" : "5", "data": [ { "x": 5}, { "x": -5} ]},
          {"id" : "6", "data": [ { "x": 6}, { "x": -6} ]},
          {"id" : "7", "data": [ { "x": 7}, { "x": -7} ]}
        ]
    """
    )

    val map = json.coerceNonNull<List<Value>>().associate { it.id to it }.toMutableMap()
    val dao = MapBackedSyncDao<Value>(FnIdMapper("id", { it.id }), map)

    @Test
    fun testSearchArray() {
        val results = dao.search(SearchRequest(limit = 10, fields = listOf(Field("data.x")), query = RangeQuery("data.x", gte = 4)))
        assertEquals(4, results.totalItems)
    }

    data class Value(val id: String, val data: List<Any?>)

}
