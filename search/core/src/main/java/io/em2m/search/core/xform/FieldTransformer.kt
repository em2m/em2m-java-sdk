package io.em2m.search.core.xform

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.core.expr.FieldKeyHandler
import io.em2m.search.core.model.*
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.ConditionExpr
import io.em2m.simplex.model.ConstConditionExpr
import io.em2m.simplex.model.Expr
import io.em2m.simplex.model.ExprContext
import io.em2m.simplex.parser.SimplexModule
import io.em2m.utils.coerce

class FieldTransformer<T>(val simplex: Simplex, fields: List<FieldModel> = emptyList()) : Transformer<T> {

    private val objectMapper = jacksonObjectMapper().registerModule(SimplexModule(simplex))
    private val fieldModels = fields.associateBy { it.name }
    private val queryXform = FieldQueryTransformer(fieldModels)
    private val aggsXform = FieldAggTransformer(fieldModels)

    override fun transformRequest(request: SearchRequest, context: ExprContext): SearchRequest {
        val sorts = transformSorts(request.sorts)
        val fields = transformFields(request.fields)
        val query = transformQuery(request.query, context)
        val aggs = transformAggs(request.aggs, context)
        return request.copy(sorts = sorts, fields = fields, query = query, aggs = aggs)
    }

    override fun transformResult(
        request: SearchRequest,
        result: SearchResult<T>,
        context: ExprContext
    ): SearchResult<T> {
        val aggResults = transformAggResults(request, result.aggs, context)
        val models = reqModels(request.fields)
        val rows = transformRows(request, result, models, context)
        return result.copy(aggs = aggResults, rows = rows, fields = request.fields)
    }

    override fun transformQuery(query: Query?, context: ExprContext): Query? {
        return query?.let { queryXform.transform(query, context) } ?: query
    }

    private fun transformAggs(aggs: List<Agg>, context: ExprContext): List<Agg> {
        return aggs.map { aggsXform.transform(it, context) }
    }

    private fun transformFields(fields: List<Field>): List<Field> {

        // 1. map expression into fields
        // 2. apply aliases
        // 3. remove duplicates

        return fields.flatMap { field ->
            if (field.expr != null) {
                val expr = simplex.parser.parse(field.expr)
                FieldKeyHandler.fields(expr).map { Field(it) }
            } else {
                listOf(field)
            }
        }.flatMap { field ->
            val name = field.name
            if (name != null) {
                val model = fieldModels[name]
                if (model != null) {
                    if (model.delegateExpr != null) {
                        listOf(Field(expr = model.delegateExpr))
                    } else {
                        model.delegateFields.map { Field(it) }
                    }
                } else {
                    listOf(field)
                }
            } else {
                listOf(field)
            }
        }.distinct()
    }

    private fun transformSorts(sorts: List<DocSort>): List<DocSort> {
        return sorts.flatMap { sort ->
            val model = fieldModels[sort.field]
            if (model?.delegateFields?.isNotEmpty() == true) {
                model.delegateFields.map { DocSort(it, sort.direction) }
            } else listOf(sort)
        }
    }

    private fun transformAggResults(req: SearchRequest, aggs: Map<String, AggResult>, context: ExprContext): Map<String, AggResult> {
        val reqAggs = req.aggs.associateBy { it.key }
        return aggs.mapValues { (key, aggResult) ->
            val agg = reqAggs[key]
            val missing = (agg as? TermsAgg)?.missing
            val expr = (agg as? TermsAgg)?.format ?: (agg as? HistogramAgg)?.format
            val scope: Map<String, Any?> = agg?.extensions ?: emptyMap()
            object : AggResultTransformer() {

                override fun transformBucket(bucket: Bucket): Bucket {
                    return if (key != missing && expr != null) {
                        val bucketContext = BucketContext(req, scope, bucket)
                        val label = simplex.eval(expr, bucketContext.toMap().plus(context)).toString()
                        bucket.copy(label = label)
                    } else bucket
                }

                override fun transform(aggResult: AggResult): AggResult {
                    val bucketFilter: ConditionExpr = scope["filterBuckets"]?.coerce(objectMapper = objectMapper)
                        ?: ConstConditionExpr(value = true)
                    val buckets = aggResult.buckets
                        ?.filter { bucket ->
                            val bucketContext = context.toMutableMap() + BucketContext(req, scope, bucket).toMap()
                            try {
                                bucketFilter.call(bucketContext)
                            } catch (e: Exception) {
                                true
                            }
                        }?.map { transformBucket(it) }
                    return if (agg is Fielded) {
                        aggResult.copy(field = agg.field, buckets = buckets)
                    } else aggResult.copy(buckets = buckets)
                }
            }.transform(aggResult)
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

    private fun transformRows(
        req: SearchRequest,
        result: SearchResult<T>,
        models: List<FieldModel>,
        context: ExprContext
    ): List<List<Any?>>? {

        return result.rows?.map { row ->
            val values = HashMap<String, Any?>()

            result.fields.forEachIndexed { index, field ->
                values[field.name ?: field.expr!!] = row[index]
            }

            // evaluate models
            val modelValues = HashMap<String, Any?>()
            models.forEach { model ->
                val exprContext = RowContext(req, context, values)
                modelValues[model.name] = when {
                    model.expr != null -> model.expr.call(exprContext.toMap().plus(model.settings))
                    model.delegateField != null -> values[model.delegateField]
                    model.delegateExpr != null -> values[model.delegateExpr]
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

    class FieldModel(
        val name: String,
        val delegateField: String? = null,
        val delegateExpr: String? = null,
        val expr: Expr? = null,
        val settings: Map<String, Any?> = emptyMap(),
        extraFields: List<String> = emptyList()
    ) {
        val delegateFields = extraFields + if (expr != null) FieldKeyHandler.fields(expr) else listOfNotNull(delegateField)
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

        override fun transformTermQuery(query: TermQuery, context: ExprContext) =
            applyAlias(query.field) { TermQuery(it, query.value) }

        override fun transformTermsQuery(query: TermsQuery, context: ExprContext) =
            applyAlias(query.field) { TermsQuery(it, query.value) }

        override fun transformMatchQuery(query: MatchQuery, context: ExprContext) =
            applyAlias(query.field) { MatchQuery(it, query.value, query.operator) }

        override fun transformPhraseQuery(query: PhraseQuery, context: ExprContext) =
            applyAlias(query.field) { PhraseQuery(it, query.value) }

        override fun transformPrefixQuery(query: PrefixQuery, context: ExprContext) =
            applyAlias(query.field) { PrefixQuery(it, query.value) }

        override fun transformWildcardQuery(query: WildcardQuery, context: ExprContext) =
            applyAlias(query.field) { WildcardQuery(it, query.value) }

        override fun transformRegexQuery(query: RegexQuery, context: ExprContext) =
            applyAlias(query.field) { RegexQuery(it, query.value) }

        override fun transformDateRangeQuery(query: DateRangeQuery, context: ExprContext) =
            applyAlias(query.field) { DateRangeQuery(it, query.lt, query.lte, query.gt, query.gte) }

        override fun transformRangeQuery(query: RangeQuery, context: ExprContext) =
            applyAlias(query.field) { RangeQuery(it, query.lt, query.lte, query.gt, query.gte) }

        override fun transformBboxQuery(query: BboxQuery, context: ExprContext) =
            applyAlias(query.field) { BboxQuery(it, query.value) }

        override fun transformExistsQuery(query: ExistsQuery, context: ExprContext) =
            applyAlias(query.field) { ExistsQuery(it, query.value) }
    }

    class FieldAggTransformer(private val fields: Map<String, FieldModel>) : AggTransformer() {

        private val queryTransformer = FieldQueryTransformer(fields)

        private fun applyAlias(field: String): String {
            return fields.getOrElse(field) { null }?.delegateFields?.firstOrNull() ?: field
        }

        override fun transformDateHistogramAgg(agg: DateHistogramAgg, context: ExprContext) = DateHistogramAgg(
            applyAlias(agg.field),
            agg.format,
            agg.interval,
            agg.offset,
            agg.timeZone,
            agg.missing,
            agg.key,
            agg.aggs,
            agg.extensions,
            agg.minDocCount
        )

        override fun transformDateRangeAgg(agg: DateRangeAgg, context: ExprContext) = DateRangeAgg(
            applyAlias(agg.field),
            agg.format,
            agg.timeZone,
            agg.ranges,
            agg.key,
            agg.aggs,
            agg.extensions,
            agg.minDocCount
        )

        override fun transformFiltersAgg(agg: FiltersAgg, context: ExprContext) = FiltersAgg(agg.filters.mapValues {
            queryTransformer.transform(it.value, context)
        }, agg.key, agg.aggs, agg.extensions, agg.minDocCount)

        override fun transformGeoBoundsAgg(agg: GeoBoundsAgg, context: ExprContext) =
            GeoBoundsAgg(applyAlias(agg.field), agg.key, agg.aggs, agg.extensions, agg.minDocCount)

        override fun transformGeoCentroidAgg(agg: GeoCentroidAgg, context: ExprContext) =
            GeoCentroidAgg(applyAlias(agg.field), agg.key, agg.aggs, agg.extensions, agg.minDocCount)

        override fun transformGeoDistanceAgg(agg: GeoDistanceAgg, context: ExprContext) = GeoDistanceAgg(
            applyAlias(agg.field),
            agg.origin,
            agg.unit,
            agg.ranges,
            agg.key,
            agg.aggs,
            agg.extensions,
            agg.minDocCount
        )

        override fun transformGeoHashAgg(agg: GeoHashAgg, context: ExprContext) = GeoHashAgg(
            applyAlias(agg.field),
            agg.precision,
            agg.size,
            agg.key,
            agg.aggs,
            agg.extensions,
            agg.minDocCount
        )

        override fun transformHistogramAgg(agg: HistogramAgg, context: ExprContext) = HistogramAgg(
            applyAlias(agg.field),
            null, /*agg.format,*/ // issue with ags not supporting format
            agg.interval,
            agg.offset,
            agg.key,
            agg.missing,
            agg.aggs,
            agg.extensions,
            agg.minDocCount
        )

        override fun transformMissingAgg(agg: MissingAgg, context: ExprContext) =
            MissingAgg(applyAlias(agg.field), agg.key, agg.aggs, agg.extensions, agg.minDocCount)

        override fun transformRangeAgg(agg: RangeAgg, context: ExprContext) =
            RangeAgg(applyAlias(agg.field), agg.ranges, agg.key, agg.aggs, agg.extensions, agg.minDocCount)

        override fun transformStatsAgg(agg: StatsAgg, context: ExprContext) =
            StatsAgg(applyAlias(agg.field), agg.key, null /*agg.format*/, agg.aggs, agg.extensions, agg.minDocCount)

        override fun transformTermsAgg(agg: TermsAgg, context: ExprContext) = TermsAgg(
            applyAlias(agg.field),
            agg.size,
            agg.key,
            agg.sort,
            null /*agg.format*/,
            agg.missing,
            agg.aggs,
            agg.extensions,
            agg.minDocCount
        )
    }
}
