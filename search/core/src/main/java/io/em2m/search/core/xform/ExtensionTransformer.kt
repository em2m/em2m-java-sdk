package io.em2m.search.core.xform

import io.em2m.search.core.model.*
import io.em2m.simplex.model.ExprContext

class ExtensionsTransformer(val ext: Map<String, Any?>) : AggTransformer() {

    override fun transformTermsAgg(agg: TermsAgg, context: ExprContext): Agg {
        val size = (ext["size"] as? Number)?.toInt()
        return TermsAgg(agg.field, size
                ?: agg.size, agg.key, agg.sort, agg.format, agg.missing, agg.aggs, agg.extensions, agg.minDocCount)
    }

    override fun transformDateRangeAgg(agg: DateRangeAgg, context: ExprContext): Agg {
        val timeZone = ext["timeZone"] as? String

        return DateRangeAgg(agg.field, agg.format, timeZone
                ?: agg.timeZone, agg.ranges, agg.key, agg.aggs, agg.extensions, agg.minDocCount)
    }

    override fun transformDateHistogramAgg(agg: DateHistogramAgg, context: ExprContext): Agg {
        val interval = ext["interval"] as? String
        val timeZone = ext["timeZone"] as? String

        return DateHistogramAgg(agg.field, agg.format, interval
                ?: agg.interval, agg.offset, timeZone
                ?: agg.timeZone, agg.missing, agg.key, agg.aggs, agg.extensions, agg.minDocCount)
    }

    override fun transformHistogramAgg(agg: HistogramAgg, context: ExprContext): Agg {
        val interval = (ext["interval"] as? Number)?.toDouble()
        return HistogramAgg(agg.field, agg.format,interval
                ?: agg.interval, agg.offset, agg.key, agg.missing, agg.aggs, agg.extensions, agg.minDocCount)
    }
}
