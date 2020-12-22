package io.em2m.search.core.xform

import io.em2m.search.core.model.*
import io.em2m.simplex.model.ExprContext

@Deprecated("Use FieldTransformerDao instead")
class FieldAliasAggTransformer(val aliases: Map<String, Field>) : AggTransformer() {

    private val queryTransformer = FieldAliasQueryTransformer(aliases)

    private fun applyAlias(field: String): String {
        return aliases.getOrElse(field, { null })?.name ?: field
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

    override fun transformFiltersAgg(agg: FiltersAgg, context: ExprContext) = FiltersAgg(
        agg.filters.mapValues { queryTransformer.transform(it.value, context) },
        agg.key,
        agg.aggs,
        agg.extensions,
        agg.minDocCount
    )

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

    override fun transformGeoHashAgg(agg: GeoHashAgg, context: ExprContext) =
        GeoHashAgg(applyAlias(agg.field), agg.precision, agg.size, agg.key, agg.aggs, agg.extensions, agg.minDocCount)

    override fun transformHistogramAgg(agg: HistogramAgg, context: ExprContext) = HistogramAgg(
        applyAlias(agg.field),
        agg.format,
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
        StatsAgg(applyAlias(agg.field), agg.key, agg.format, agg.aggs, agg.extensions, agg.minDocCount)

    override fun transformTermsAgg(agg: TermsAgg, context: ExprContext) = TermsAgg(
        applyAlias(agg.field),
        agg.size,
        agg.key,
        agg.sort,
        agg.format,
        agg.missing,
        agg.aggs,
        agg.extensions,
        agg.minDocCount
    )
}
