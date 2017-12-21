package io.em2m.search.core.expr

import io.em2m.search.core.daos.SearchDaoWrapper
import io.em2m.search.core.model.*
import io.em2m.search.core.xform.AggResultTransformer
import io.em2m.simplex.model.Expr
import io.em2m.simplex.model.KeyResolver
import io.em2m.simplex.model.PipeTransformResolver
import io.em2m.simplex.parser.ExprParser
import rx.Observable

class ExprTransformingSearchDao<T>(
        keyResolver: KeyResolver,
        pipeTransformResolver: PipeTransformResolver,
        delegate: SearchDao<T>) : SearchDaoWrapper<T>(delegate) {

    val parser = ExprParser(keyResolver, pipeTransformResolver)

    override fun search(request: SearchRequest): Observable<SearchResult<T>> {

        val rowExprs = request.fields.map { it.expr?.let { parser.parse(it) } }
        val rowNames = request.fields.map {
            if (it.expr == null && it.name != null) {
                it.name
            } else null
        }
        val exprFields = rowExprs.filterNotNull().flatMap { FieldKeyHandler.fields(it) }
        val delegateFields = exprFields.plus(rowNames).filterNotNull().map { Field(name = it) }
        return delegate.search(request.copy(fields = delegateFields)).map { results ->
            val rows = transformRows(request, results.rows, delegateFields, rowExprs)
            val aggs = transformAggResults(request, results.aggs)
            results.copy(fields = request.fields, rows = rows, aggs = aggs)
        }
    }

    fun transformRows(request: SearchRequest, rows: List<List<*>>?, delegateFields: List<Field>, exprs: List<Expr?>): List<List<*>>? {
        return if (rows != null && rows.isNotEmpty()) {
            rows.map { row: List<*> ->
                val values = HashMap<String, Any?>()
                (0..delegateFields.lastIndex).forEach { i ->
                    val name = delegateFields[i].name
                    if (name != null) {
                        values.put(name, row[i])
                    }
                }
                val exprContext = RowContext(request, emptyMap(), values)
                request.fields.withIndex().map {
                    val index = it.index
                    val name = it.value.name
                    val expr = exprs[index]
                    if (expr != null) {
                        expr.call(exprContext.map)
                    } else if (name != null) {
                        values[name]
                    } else null
                }
            }
        } else rows
    }

    fun transformAggResults(request: SearchRequest, aggResults: Map<String, AggResult>): Map<String, AggResult> {

        val aggExprs = HashMap<String, Expr?>()
        val missings = HashMap<String, String>()
        request.aggs.forEach {
            val termsAgg = it as? TermsAgg
            if (termsAgg != null && termsAgg.format != null) {
                aggExprs.put(termsAgg.key, parser.parse(termsAgg.format))
                if (termsAgg.missing != null) {
                    missings.put(termsAgg.key, termsAgg.missing)
                }
            }
        }

        return aggResults.mapValues { (key, aggResult) ->
            val expr = aggExprs[key]
            val missing = missings[key]
            val scope = emptyMap<String, Any>()
            // Do not transform is it's the missing key or if no expression
            if (key != missing && expr != null) {
                val xformer = object : AggResultTransformer() {
                    override fun transformBucket(bucket: Bucket): Bucket {
                        val context = BucketContext(request, scope, bucket)
                        val label = expr.call(context.map).toString()
                        return Bucket(key = bucket.key, count = bucket.count, stats = bucket.stats, label = label)
                    }
                }
                xformer.transform(aggResult)
            } else aggResult
        }
    }

}