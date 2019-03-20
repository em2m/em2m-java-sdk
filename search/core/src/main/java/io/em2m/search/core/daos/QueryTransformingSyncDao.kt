package io.em2m.search.core.daos

import io.em2m.search.core.model.*
import io.em2m.search.core.xform.*

class QueryTransformingSyncDao<T>(
        private val aliases: Map<String, Field> = emptyMap(),
        private val fieldSets: Map<String, List<Field>> = emptyMap(),
        private val namedAggs: Map<String, Agg> = emptyMap(),
        delegate: SyncDao<T>) : SyncDaoWrapper<T>(delegate) {

    override fun search(request: SearchRequest): SearchResult<T> {
        val result = super.search(transformRequest(request)).copy(fields = request.fields)
        return transformResult(request, result)
    }

    override fun count(query: Query): Long {
        return super.count(transformQuery(query))
    }

    override fun findOne(query: Query): T? {
        return super.findOne(transformQuery(query))
    }

    private fun transformQuery(query: Query): Query {
        return query
                .let { LuceneQueryTransformer().transform(it) }
                .let { FieldAliasQueryTransformer(aliases).transform(it) }
                .let { NamedAggQueryTransformer(namedAggs).transform(it) }
    }

    private fun transformAggs(aggs: List<Agg>): List<Agg> {
        val aliasXform = FieldAliasAggTransformer(aliases)
        val namedXform = NamedAggTransformer(namedAggs)
        return aggs
                .map {
                    aliasXform.transform(it)
                }
                .map {
                    namedXform.transform((it))
                }
    }

    private fun transformSorts(sorts: List<DocSort>): List<DocSort> {
        return sorts.map {
            val alias = aliases[it.field]
            DocSort(alias?.expr ?: alias?.name ?: it.field, it.direction)
        }
    }

    private fun transformRequest(request: SearchRequest): SearchRequest {
        val fields = request.fields
                .plus(fieldSets[request.fieldSet] ?: emptyList())
                .map {
                    val name = it.name
                    if (name != null) {
                        val alias = aliases[name]
                        if (alias != null) {
                            val label = it.label ?: alias.label
                            val settings = it.settings
                            Field(name = alias.name, expr = alias.expr, label = label, settings = settings)
                        } else it
                    } else it
                }
        val sorts = transformSorts(request.sorts)
        val query = request.query?.let { transformQuery(it) }
        val aggs = transformAggs(request.aggs)
        val fieldSet = if (fieldSets.containsKey(request.fieldSet)) null else request.fieldSet
        return request.copy(fieldSet = fieldSet, fields = fields, sorts = sorts, query = query, aggs = aggs)
    }

    private fun transformResult(request: SearchRequest, result: SearchResult<T>): SearchResult<T> {
        val aggs = transformAggResults(request, result.aggs)
        return result.copy(aggs = aggs)
    }

    private fun transformAggResults(request: SearchRequest, aggResults: Map<String, AggResult>): Map<String, AggResult> {
        return request.aggs.mapNotNull { agg ->
            val aggResult = aggResults[agg.key]
            if (agg is NamedAgg) {
                val named = namedAggs[agg.name]
                when (named) {
                    is Fielded -> aggResult?.copy(field = named.field)
                    is FiltersAgg -> aggResult?.copy(buckets = transformFilterBuckets(named, aggResult.buckets))
                    else -> aggResult
                }
            } else aggResult
        }.associateBy { it.key }
    }

    private fun transformFilterBuckets(agg: FiltersAgg, buckets: List<Bucket>?): List<Bucket>? {
        if (buckets == null) return null
        return buckets.map { bucket ->
            val key = bucket.key
            val query = agg.filters[key]
            bucket.copy(query = query)
        }
    }

}