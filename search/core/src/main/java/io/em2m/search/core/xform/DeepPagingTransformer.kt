package io.em2m.search.core.xform

import io.em2m.search.core.model.*
import io.em2m.search.core.model.Direction.Descending
import io.em2m.simplex.model.ExprContext
import io.em2m.utils.coerce

class DeepPagingTransformer<T>(private val idField: String) : Transformer<T> {

    override fun transformRequest(request: SearchRequest, context: ExprContext): SearchRequest {
        return if (request.deepPage && request.fields.isNotEmpty()) {
            val last: Map<String, Any?>? = request.params["lastKey"].coerce()
            val sorts = transformSorts(request.sorts)
            val fields = transformFields(request.fields)
            val query = transformQuery(request.query, sorts, last)
            val aggs = request.aggs
            request.copy(sorts = sorts, fields = fields, query = query, aggs = aggs)
        } else {
            request
        }
    }

    private fun transformQuery(query: Query?, sorts: List<DocSort>, last: Map<String, Any?>?): Query? {
        if (last == null) {
            return query
        }
        /*
         (A,B, ID) ->
              (A > AX)
              (A == AX) AND (B > BX)
              (A == AX) AND (B == BX) AND (ID > IDX)
         */
        // todo - validate last contains correct keys

        val remainingSorts = sorts.reversed().toMutableList()
        val queries = ArrayList<Query>()
        while (remainingSorts.isNotEmpty()) {
            val head = remainingSorts.removeFirst()
            queries.add(
                AndQuery(
                    if (head.direction == Descending) RangeQuery(
                        field = head.field,
                        lt = last[head.field]
                    ) else RangeQuery(field = head.field, gt = last[head.field]),
                    AndQuery(remainingSorts.map { TermQuery(field = it.field, value = last[it.field]) })
                )
            )
        }
        return AndQuery(query ?: MatchAllQuery(), OrQuery(queries)).simplify()
    }

    private fun transformFields(fields: List<Field>): List<Field> {
        // todo - check and see if fields already contains id field
        return fields.plus(Field(name = idField))
    }

    private fun transformSorts(sorts: List<DocSort>): List<DocSort> {
        // TODO - check and see if the sort already contains ID field
        return sorts.plus(DocSort(idField))
    }

    override fun transformResult(
        request: SearchRequest,
        result: SearchResult<T>,
        context: ExprContext
    ): SearchResult<T> {
        val lastRow = result.rows?.lastOrNull()
        return if (request.deepPage && request.fields.isNotEmpty() && lastRow != null) {
            val fieldIndex = result.fields.mapIndexed { index, field ->
                field.name to index
            }.toMap()
            val keys = request.sorts.map { it.field }.toSet().plus(idField)
            val lastKey = keys.mapNotNull { key ->
                val pos = fieldIndex[key]
                if (pos != null) {
                    key to lastRow[pos]
                } else null
            }.toMap()
            result.copy(headers = result.headers.plus("lastKey" to lastKey))
        } else result
    }
}
