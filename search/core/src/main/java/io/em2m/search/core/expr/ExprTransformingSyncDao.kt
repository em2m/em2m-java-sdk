package io.em2m.search.core.expr

import io.em2m.search.core.daos.SyncDaoWrapper
import io.em2m.search.core.model.*
import io.em2m.search.core.xform.SourceFormatAggTransformer
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.Expr

open class ExprTransformingSyncDao<T>(simplex: Simplex, delegate: SyncDao<T>) : SyncDaoWrapper<T>(delegate) {

    val parser = simplex.parser

    override fun search(request: SearchRequest): SearchResult<T> {

        val rowExprs = request.fields.map { it.expr?.let { expr -> parser.parse(expr) } }
        val rowNames = request.fields.map {
            if (it.expr == null && it.name != null) {
                it.name
            } else null
        }
        val exprFields = rowExprs.filterNotNull().flatMap { FieldKeyHandler.fields(it) }
        val delegateFields = exprFields.plus(rowNames).filterNotNull().map { Field(name = it) }
        val query = request.query?.let { transformQuery(it) }
        val req = request.copy(query = query, fields = delegateFields, aggs = transformAggs(request.aggs), sorts = transformSorts(request.sorts))
        return delegate.search(req).let { results ->
            val rows = transformRows(request, results.rows, delegateFields, rowExprs)
            val aggs = transformAggResults(request, results.aggs)
            results.copy(fields = request.fields, rows = rows, aggs = aggs)
        }
    }

    private fun transformSorts(sorts: List<DocSort>): List<DocSort> {
        return sorts.flatMap { sort ->
            if (sort.field.contains("\${")) {
                val expr = parser.parse(sort.field)
                val fields = FieldKeyHandler.fields(expr)
                fields.map { field ->
                    DocSort(field, sort.direction)
                }
            } else {
                listOf(sort)
            }
        }
    }

    private fun transformQuery(query: Query, timeZone: String? = null): Query {
        return query.let { ExprQueryTransformer(parser).transform(it) }
    }

    private fun transformAggs(aggs: List<Agg>): List<Agg> {
        val sourceFormatXform = SourceFormatAggTransformer()
        return aggs.map {
            sourceFormatXform.transform((it))
        }
    }

    private fun transformRows(request: SearchRequest, rows: List<List<*>>?, delegateFields: List<Field>, exprs: List<Expr?>): List<List<*>>? {
        return if (rows != null && rows.isNotEmpty()) {
            rows.map { row: List<*> ->
                val values = HashMap<String, Any?>()
                (0..delegateFields.lastIndex).forEach { i ->
                    val name = delegateFields[i].name
                    if (name != null) {
                        values[name] = row[i]
                    }
                }
                val exprContext = RowContext(request, emptyMap(), values)
                request.fields.withIndex().map {
                    val index = it.index
                    val name = it.value.name
                    val expr = exprs[index]
                    val settings = it.value.settings
                    when {
                        expr != null -> expr.call(exprContext.map.plus(settings))
                        name != null -> values[name]
                        else -> null
                    }
                }
            }
        } else rows
    }

    private fun transformAggResults(request: SearchRequest, aggResults: Map<String, AggResult>): Map<String, AggResult> {

        val aggExprs = HashMap<String, Expr?>()
        val aggMap = HashMap<String, Agg>()
        val missings = HashMap<String, Any?>()
        request.aggs.forEach {
            aggMap[it.key] = it
            val termsAgg = it as? TermsAgg
            val statsAgg = it as? StatsAgg
            val format = termsAgg?.format ?: statsAgg?.format
            if (format != null) {
                aggExprs[it.key] = parser.parse(format)
                if (termsAgg?.missing != null) {
                    missings[it.key] = termsAgg.missing
                }
            }
        }

        return aggResults.mapValues { (key, aggResult) ->
            val agg = aggMap[key]
            val expr = aggExprs[key]
            val missing = missings[key]
            val scope = agg?.extensions ?: emptyMap<String, Any?>()
            // Do not transform is it's the missing key or if no expression
            if (key != missing && expr != null) {
                val xformer = object : AggResultTransformer() {
                    override fun transformBucket(bucket: Bucket): Bucket {
                        val context = BucketContext(request, scope, bucket)
                        // temporarily move scope up a level until we have a better fix
                        val label = expr.call(context.map.plus(scope)).toString()
                        return bucket.copy(label = label)
                    }
                }
                xformer.transform(aggResult)
            } else aggResult
        }
    }
}