package io.em2m.search.core.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.ObjectMapper

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("headers", "totalItems", "fields", "aggs", "rows", "items")
data class SearchResult<out T>(val aggs: Map<String, AggResult> = emptyMap(),
                               val items: List<T>? = null, val rows: List<List<Any?>>? = null, val totalItems: Long,
                               val headers: Map<String, Any?> = emptyMap(), val fields: List<Field> = emptyList()) {

    fun <TT> transformItems(xform: (T) -> TT): SearchResult<TT> {
        return SearchResult(aggs, items?.map(xform), rows, totalItems, headers, fields)
    }

    companion object {

        fun <T> combineSearchResults(results: List<SearchResult<T>>, rowToItemFn: ((List<List<Any?>>?) -> List<T>)? = null): SearchResult<T> {

            // finding the total items count is the same problem as finding the
            // count for a specific query. this "totalItems" number could be over the limit for elasticsearch

            val useItems = results.any { result -> result.items != null }

            if (useItems) {
                val items = results.flatMapTo(mutableListOf()) {
                    result -> result.items ?: rowToItemFn?.invoke(result.rows) ?: emptyList()
                }.distinct()
                return SearchResult(totalItems = items.size.toLong(), items = items)
            }

            val rows = results.flatMap { result ->
                result.rows ?: emptyList()
            }.distinct()
            return SearchResult(totalItems = rows.size.toLong(), rows = rows)

        }

    }

}
