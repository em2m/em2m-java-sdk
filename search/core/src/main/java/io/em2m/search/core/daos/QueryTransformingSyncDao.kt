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
        return super.search(transformRequest(request)).copy(fields = request.fields)
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
        val sorts = request.sorts.map { DocSort(aliases.get(it.field)?.name ?: it.field, it.direction) }
        val query = request.query?.let { transformQuery(it) }
        val aggs = transformAggs(request.aggs)
        val fieldSet = if (fieldSets.containsKey(request.fieldSet)) null else request.fieldSet
        return request.copy(fieldSet = fieldSet, fields = fields, sorts = sorts, query = query, aggs = aggs)
    }

}