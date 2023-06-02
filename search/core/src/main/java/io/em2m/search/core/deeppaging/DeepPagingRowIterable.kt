package io.em2m.search.core.deeppaging

import io.em2m.search.core.model.*
import io.em2m.search.core.xform.DeepPagingTransformer
import io.em2m.utils.coerce

class DeepPagingRowIterable(
    val searchable: Searchable<*>,
    val chunkSize: Long,
    val idField: String = "_id",
    val fields: List<Field>,
    val query: Query = MatchAllQuery(),
    val sorts: List<DocSort> = emptyList(),
    val params: Map<String, Any> = emptyMap()
): Iterable<List<Any?>> {
    private val totalItems = fetchTotalItems()

    fun count(): Long {
        return totalItems
    }

    override fun iterator(): Iterator<List<Any?>> {
        return DpRowIterator()
    }

    private fun fetchTotalItems(): Long {
        val totalItemsRequest = SearchRequest(
            offset = 0,
            limit = 0,
            query = query,
            sorts = sorts
        )
        return searchable.search(totalItemsRequest).totalItems
    }

    inner class DpRowIterator: Iterator<List<Any?>> {
        private val deepPagingTransformer = DeepPagingTransformer<Any?>(idField)
        private var done = false
        private var moreItemsToFetch = true
        private var buffer = ArrayDeque<List<Any?>>()
        private var lastKey: Map<String, Any?> = emptyMap()

        override fun hasNext(): Boolean {
            return !done && totalItems > 0
        }

        override fun next(): List<Any?> {
            if (!done && buffer.isEmpty()) fetchMore()
            if (buffer.isNotEmpty()) {
                val nextItem = buffer.removeFirst()
                done = buffer.isEmpty() && !moreItemsToFetch
                return nextItem
            } else {
                done = true
                throw NoSuchElementException("Buffer was empty, and no new items could be found")
            }
        }

        private fun fetchMore() {
            val initialRequest = SearchRequest(
                offset = 0,
                limit = chunkSize,
                fields = fields,
                query = query,
                sorts = sorts
            )

            val transformedParams = if (lastKey.isNotEmpty()) {
                params.plus("lastKey" to lastKey).plus("deepPage" to true)
            } else {
                params.plus("deepPage" to true)
            }
            initialRequest.params = transformedParams

            val transformedRequest = deepPagingTransformer.transformRequest(initialRequest, emptyMap())
            val searchResult = searchable.search(transformedRequest)

            val transformedResult = deepPagingTransformer.transformResult(transformedRequest, searchResult, emptyMap())
            val totalItems = transformedResult.totalItems
            val newRows = transformedResult.rows ?: emptyList()

            buffer.addAll(newRows)
            lastKey = transformedResult.headers["lastKey"]?.coerce() ?: emptyMap()
            moreItemsToFetch = newRows.size < totalItems
        }

    }
}
