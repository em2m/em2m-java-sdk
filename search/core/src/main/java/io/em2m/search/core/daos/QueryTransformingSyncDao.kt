package io.em2m.search.core.daos

import io.em2m.search.core.model.*
import io.em2m.search.core.xform.FieldAliasAggTransformer
import io.em2m.search.core.xform.FieldAliasQueryTransformer
import io.em2m.search.core.xform.LuceneQueryTransformer
import io.em2m.search.core.xform.NamedAggTransformer

class QueryTransformingSyncDao<T>(
        val aliases: Map<String, Field> = emptyMap(),
        val fieldSets: Map<String, List<Field>> = emptyMap<String, List<Field>>(),
        val namedAggs: Map<String, Agg> = emptyMap(),
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

    fun transformQuery(query: Query): Query {
        return query
                .let { LuceneQueryTransformer().transform(it) }
                .let { FieldAliasQueryTransformer(aliases).transform(it) }
    }

    fun transformAggs(aggs: List<Agg>): List<Agg> {
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

    fun transformSorts(sorts: List<DocSort>): List<DocSort> {
        return sorts.map {
            val alias = aliases[it.field]
            DocSort(alias?.expr ?: alias?.name ?: it.field, it.direction)
        }
    }

    fun transformRequest(request: SearchRequest): SearchRequest {
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
                aggResult?.copy(field = (named as? Fielded)?.field)
            } else aggResult
        }.associateBy { it.key }
    }


}