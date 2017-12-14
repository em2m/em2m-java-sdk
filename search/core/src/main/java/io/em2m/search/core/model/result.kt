package io.em2m.search.core.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("headers", "totalItems", "fields", "aggs", "rows", "items")
data class SearchResult<out T>(val aggs: Map<String, AggResult> = emptyMap(),
                               val items: List<T>? = null, val rows: List<List<Any?>>? = null, val totalItems: Long,
                               val headers: Map<String, Any?> = emptyMap(), val fields: List<Field> = emptyList()) {

    fun <TT> transformItems(xform: (T) -> TT): SearchResult<TT> {
        return SearchResult(aggs, items?.map(xform), rows, totalItems, headers, fields)
    }

}
