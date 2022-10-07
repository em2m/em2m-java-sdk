package io.em2m.search.core.xform

import io.em2m.search.core.model.*
import io.em2m.search.core.model.Direction.Descending
import io.em2m.simplex.evalPath
import io.em2m.simplex.model.ExprContext
import io.em2m.utils.coerce

class DeepPagingTransformer<T>(private val idField: String) : Transformer<T> {

    override fun transformRequest(request: SearchRequest, context: ExprContext): SearchRequest {
        return if (request.deepPage) {
            val last: Map<String, Any?>? = request.params["lastKey"].coerce()
            val sorts = transformSorts(request.sorts)
            val fields = if (request.fields.isNotEmpty()) transformFields(request.fields) else request.fields
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
        return if (fields.map { it.name }.contains(idField))
            fields
        else
            fields.plus(Field(name = idField))
    }

    private fun transformSorts(sorts: List<DocSort>): List<DocSort> {
        return if (sorts.map { it.field }.contains(idField))
            sorts
        else
            sorts.plus(DocSort(idField))
    }

    override fun transformResult(
        request: SearchRequest,
        result: SearchResult<T>,
        context: ExprContext
    ): SearchResult<T> {
        val lastRow = result.rows?.lastOrNull()
        val lastItem = result.items?.lastOrNull()
        return if (request.deepPage && lastRow != null) {
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
        } else if (request.deepPage && lastItem != null) {
            val keys = request.sorts.map { it.field }.toSet().plus(idField)
            val lastKey = keys.associateWith { key ->
                lastItem.evalPath(key)
            }
            result.copy(headers = result.headers.plus("lastKey" to lastKey))
        } else result
    }
}
