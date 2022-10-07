package io.em2m.search.core.deeppaging

import io.em2m.search.core.model.*
import io.em2m.search.core.xform.DeepPagingTransformer
import io.em2m.utils.coerce

class DeepPagingItemIterable<T>(
    val searchable: Searchable<T>,
    val chunkSize: Long,
    val idField: String = "_id",
    val query: Query = MatchAllQuery(),
    val sorts: List<DocSort> = emptyList(),
    var params: Map<String, Any> = emptyMap(),
) : Iterable<T> {
    private val totalItems = fetchTotalItems()

    fun count(): Long {
        return totalItems
    }

    override fun iterator(): Iterator<T> {
        return DpItemIterator()
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

    inner class DpItemIterator : Iterator<T> {
        private val deepPagingTransformer = DeepPagingTransformer<T>(idField)
        private var done = false
        private var moreItemsToFetch = true
        private var buffer = ArrayDeque<T>()
        private var lastKey: Map<String, Any?> = emptyMap()

        override fun hasNext(): Boolean {
            return !done
        }

        override fun next(): T {
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
                query = query,
                sorts = sorts,
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
            val newItems = transformedResult.items ?: emptyList()
            val totalItems = transformedResult.totalItems

            buffer.addAll(newItems)
            lastKey = transformedResult.headers["lastKey"]?.coerce() ?: emptyMap()
            moreItemsToFetch = newItems.size < totalItems
        }

    }
}
