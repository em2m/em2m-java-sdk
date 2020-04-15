package io.em2m.search.core.xform

import io.em2m.search.core.expr.FieldKeyHandler
import io.em2m.search.core.model.*
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.Expr

class FieldTransformer<T>(val simplex: Simplex, fields: List<FieldModel>) : Transformer<T> {

    private val fieldModels = fields.associateBy { it.name }
    private val queryXform = FieldQueryTransformer(fieldModels)
    private val aggsXform = FieldAggTransformer(fieldModels)

    override fun transformRequest(request: SearchRequest): SearchRequest {
        val sorts = transformSorts(request.sorts)
        val fields = transformFields(request.fields)
        val query = transformQuery(request.query)
        val aggs = transformAggs(request.aggs)
        return request.copy(sorts = sorts, fields = fields, query = query, aggs = aggs)
    }

    override fun transformResult(request: SearchRequest, result: SearchResult<T>): SearchResult<T> {
        val aggResults = transformAggResults(request, result.aggs)
        val models = reqModels(request.fields)
        val fields = delegateFields(models)
        val rows = transformRows(request, models, fields, result.rows)
        return result.copy(aggs = aggResults, rows = rows, fields = request.fields)
    }

    override fun transformQuery(query: Query?): Query? {
        return query?.let { queryXform.transform(query) } ?: query
    }


    private fun transformAggs(aggs: List<Agg>): List<Agg> {
        return aggs.map { aggsXform.transform(it) }
    }

    private fun transformFields(fields: List<Field>): List<Field> {

        // 1. map expression into fields
        // 2. apply aliases
        // 3. remove duplicates

        return fields.flatMap { field ->
            if (field.expr != null) {
                val expr = simplex.parser.parse(field.expr)
                val delegates = FieldKeyHandler.fields(expr)
                FieldKeyHandler.fields(expr).map { Field(it) }
            } else {
                listOf(field)
            }
        }.flatMap { field ->
            val name = field.name
            if (name != null && fieldModels.containsKey(name)) {
                fieldModels[name]?.delegateFields?.map { Field(it) } ?: listOf(field)
            } else {
                listOf(field)
            }
        }.distinct()
    }

    private fun transformSorts(sorts: List<DocSort>): List<DocSort> {
        return sorts.flatMap { sort ->
            val model = fieldModels[sort.field]
            if (model?.delegateFields != null) {
                model.delegateFields.map { DocSort(it) }
            } else listOf(sort)
        }
    }

    private fun transformAggResults(req: SearchRequest, aggs: Map<String, AggResult>): Map<String, AggResult> {
        val reqAggs = req.aggs.associateBy { it.key }
        return aggs.mapValues { (key, aggResult) ->
            val agg = reqAggs[key]
            val missing = (agg as? TermsAgg)?.missing
            val expr = (agg as? TermsAgg)?.format
            val scope: Map<String, Any?> = agg?.extensions ?: emptyMap()
            if (key != missing && expr != null) {
                object : AggResultTransformer() {
                    override fun transformBucket(bucket: Bucket): Bucket {
                        val context = BucketContext(req, scope, bucket)
                        val label = simplex.eval(expr, context.toMap().plus(scope)).toString()
                        return bucket.copy(label = label)
                    }
                }.transform(aggResult)
            } else {
                aggResult
            }
        }
    }

    private fun reqModels(fields: List<Field>): List<FieldModel> {
        return fields.flatMap { field ->
            when {
                field.expr != null -> {
                    val expr = simplex.parser.parse(field.expr)
                    FieldKeyHandler.fields(expr).map { delegate ->
                        fieldModels[delegate] ?: FieldModel(name = delegate, delegateField = delegate)
                    }
                }
                field.name != null -> {
                    listOf(fieldModels[field.name] ?: FieldModel(field.name, delegateField = field.name))
                }
                else -> emptyList()
            }
        }
    }

    private fun delegateFields(models: List<FieldModel>): List<Field> {
        return models.flatMap { model ->
            if (model.delegateExpr != null) {
                listOf(Field(expr = model.delegateExpr))
            } else {
                model.delegateFields.map { delegate ->
                    Field(name = delegate)
                }
            }
        }.distinct()
    }

    private fun transformRows(req: SearchRequest, models: List<FieldModel>, delegates: List<Field>, rows: List<List<Any?>>?): List<List<Any?>>? {
        return rows?.map { row ->
            val values = HashMap<String, Any?>()

            delegates.forEachIndexed { index, field ->
                values[field.name ?: field.expr!!] = row[index]
            }

            // evaluate models
            val modelValues = HashMap<String, Any?>()
            models.forEach { model ->
                val exprContext = RowContext(req, emptyMap(), values)
                modelValues[model.name] = when {
                    model.expr != null -> model.expr.call(exprContext.toMap().plus(model.settings))
                    model.delegateField != null -> values[model.delegateField]
                    else -> null
                }
            }
            req.fields.map { field ->
                val exprContext = RowContext(req, emptyMap(), modelValues)
                if (field.expr != null) {
                    simplex.eval(field.expr, exprContext.toMap().plus(field.settings))
                } else {
                    modelValues[field.name]
                }
            }
        }
    }

    class FieldModel(val name: String,
                     val delegateField: String? = null,
                     val delegateExpr: String? = null,
                     val expr: Expr? = null,
                     val settings: Map<String, Any?> = emptyMap()) {
        val delegateFields = if (expr != null) FieldKeyHandler.fields(expr) else listOfNotNull(delegateField)
    }

    class FieldQueryTransformer(private val aliases: Map<String, FieldModel>) : QueryTransformer() {

        private fun applyAlias(field: String, fn: (String) -> Query): Query {
            val fields = aliases[field]?.delegateFields ?: listOf(field)
            val queries = fields.map {
                fn(it)
            }
            return when (queries.size) {
                0 -> MatchAllQuery()
                1 -> queries.first()
                else -> {
                    OrQuery(queries)
                }
            }
        }

        override fun transformTermQuery(query: TermQuery) = applyAlias(query.field) { TermQuery(it, query.value) }
        override fun transformTermsQuery(query: TermsQuery) = applyAlias(query.field) { TermsQuery(it, query.value) }
        override fun transformMatchQuery(query: MatchQuery) = applyAlias(query.field) { MatchQuery(it, query.value, query.operator) }
        override fun transformPhraseQuery(query: PhraseQuery) = applyAlias(query.field) { PhraseQuery(it, query.value) }
        override fun transformPrefixQuery(query: PrefixQuery) = applyAlias(query.field) { PrefixQuery(it, query.value) }
        override fun transformWildcardQuery(query: WildcardQuery) = applyAlias(query.field) { WildcardQuery(it, query.value) }
        override fun transformRegexQuery(query: RegexQuery) = applyAlias(query.field) { RegexQuery(it, query.value) }
        override fun transformDateRangeQuery(query: DateRangeQuery) = applyAlias(query.field) { DateRangeQuery(it, query.lt, query.lte, query.gt, query.gte) }
        override fun transformRangeQuery(query: RangeQuery) = applyAlias(query.field) { RangeQuery(it, query.lt, query.lte, query.gt, query.gte) }
        override fun transformBboxQuery(query: BboxQuery) = applyAlias(query.field) { BboxQuery(it, query.value) }
        override fun transformExistsQuery(query: ExistsQuery) = applyAlias(query.field) { ExistsQuery(it, query.value) }
    }

    class FieldAggTransformer(private val fields: Map<String, FieldModel>) : AggTransformer() {

        private val queryTransformer = FieldQueryTransformer(fields)

        private fun applyAlias(field: String): String {
            return fields.getOrElse(field, { null })?.delegateFields?.firstOrNull() ?: field
        }

        override fun transformDateHistogramAgg(agg: DateHistogramAgg) = DateHistogramAgg(applyAlias(agg.field), agg.format, agg.interval, agg.offset, agg.timeZone, agg.missing, agg.key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformDateRangeAgg(agg: DateRangeAgg) = DateRangeAgg(applyAlias(agg.field), agg.format, agg.timeZone, agg.ranges, agg.key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformFiltersAgg(agg: FiltersAgg) = FiltersAgg(agg.filters.mapValues { queryTransformer.transform(it.value) ?: MatchAllQuery()}, agg.key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoBoundsAgg(agg: GeoBoundsAgg) = GeoBoundsAgg(applyAlias(agg.field), agg.key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoCentroidAgg(agg: GeoCentroidAgg) = GeoCentroidAgg(applyAlias(agg.field), agg.key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoDistanceAgg(agg: GeoDistanceAgg) = GeoDistanceAgg(applyAlias(agg.field), agg.origin, agg.unit, agg.ranges, agg.key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformGeoHashAgg(agg: GeoHashAgg) = GeoHashAgg(applyAlias(agg.field), agg.precision, agg.size, agg.key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformHistogramAgg(agg: HistogramAgg) = HistogramAgg(applyAlias(agg.field), agg.interval, agg.offset, agg.key, agg.missing, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformMissingAgg(agg: MissingAgg) = MissingAgg(applyAlias(agg.field), agg.key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformRangeAgg(agg: RangeAgg) = RangeAgg(applyAlias(agg.field), agg.ranges, agg.key, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformStatsAgg(agg: StatsAgg) = StatsAgg(applyAlias(agg.field), agg.key, agg.format, agg.aggs, agg.extensions, agg.minDocCount)
        override fun transformTermsAgg(agg: TermsAgg) = TermsAgg(applyAlias(agg.field), agg.size, agg.key, agg.sort, agg.format, agg.missing, agg.aggs, agg.extensions, agg.minDocCount)

    }
}