package io.em2m.search.core.xform

import io.em2m.search.core.model.*

/**
 * Provide an additional format option that is passed through to a source
 */
class SourceFormatAggTransformer : AggTransformer() {

    private fun sourceFormat(format: String?, agg: Agg): String? {
        val sourceFormat = agg.extensions["sourceFormat"]
        return if (sourceFormat is String) {
            sourceFormat
        } else format
    }

    override fun transformDateHistogramAgg(agg: DateHistogramAgg) = DateHistogramAgg(agg.field, sourceFormat(agg.format, agg), agg.interval, agg.offset, agg.timeZone, agg.missing, agg.key, agg.aggs, agg.extensions.minus("sourceFormat"), agg.minDocCount)
    override fun transformDateRangeAgg(agg: DateRangeAgg) = DateRangeAgg(agg.field, sourceFormat(agg.format, agg), agg.timeZone, agg.ranges, agg.key, agg.aggs, agg.extensions.minus("sourceFormat"), agg.minDocCount)
    override fun transformTermsAgg(agg: TermsAgg) = TermsAgg(agg.field, agg.size, agg.key, agg.sort, sourceFormat(agg.format, agg), agg.missing, agg.aggs, agg.extensions.minus("sourceFormat"), agg.minDocCount)

}