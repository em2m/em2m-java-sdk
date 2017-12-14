package io.em2m.search.core.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SearchRequest(var offset: Long = 0, var limit: Long = 10, var query: Query? = null,
                         var headers: Map<String, Any> = emptyMap(),
                         var fieldSet: String? = null,
                         var fields: List<Field> = emptyList(),
                         var sorts: List<DocSort> = emptyList(),
                         var aggs: List<Agg> = emptyList()) {

    var countTotal: Boolean = true

    fun fields(vararg fields: String): SearchRequest {
        this.fields = this.fields.plus(fields.map { Field(name = it) })
        return this
    }

    fun countTotal(value: Boolean): SearchRequest {
        this.countTotal = value
        return this
    }

    fun sort(field: String, direction: Direction? = Direction.Ascending): SearchRequest {
        sorts = sorts.plus(DocSort(field, direction))
        return this
    }

    fun aggs(vararg aggs: Agg): SearchRequest {
        this.aggs = aggs.asList()
        return this
    }

}

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class Field(val name: String? = null, val label: String? = null, val expr: String? = null, val settings: Map<String, Any> = emptyMap()) {

    init {
        require(name == null || expr == null, { "Cannot specify both name and expr" })
    }
}

enum class Direction { Ascending, Descending }

class DocSort(val field: String, val direction: Direction? = Direction.Ascending)

fun request(offset: Long = 0, limit: Long = 0, query: AndQuery? = null, init: SearchRequest.() -> Unit): SearchRequest {
    val request = SearchRequest(offset, limit, query)
    request.init()
    return request
}
