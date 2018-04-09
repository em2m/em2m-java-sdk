package io.em2m.search.core.xform

import io.em2m.search.core.model.*

class NamedAggTransformer(val namedAggs: Map<String, Agg>) : AggTransformer() {

    override fun transformNamedAgg(agg: NamedAgg): Agg {

        return namedAggs[agg.name]
                ?.let { KeyAggTransformer(agg.key).transform(it) }
                ?.let { ExtensionsTransformer(agg.extensions).transform(it) } ?: agg
    }

    class ExtensionsTransformer(val ext: Map<String, Any?>) : AggTransformer() {

        override fun transformTermsAgg(agg: TermsAgg): Agg {
            val size = (ext["size"] as? Number)?.toInt()
            return TermsAgg(agg.field, size ?: agg.size, agg.key, agg.sort, agg.format, agg.missing, agg.extensions)
        }

        override fun transformDateHistogramAgg(agg: DateHistogramAgg): Agg {
            val interval = ext["interval"] as? String
            val timeZone = ext["timeZone"] as? String

            return DateHistogramAgg(agg.field, agg.format, interval
                    ?: agg.interval, agg.offset, timeZone ?: agg.timeZone, agg.key, agg.missing, agg.extensions)
        }

        override fun transformHistogramAgg(agg: HistogramAgg): Agg {
            val interval = (ext["interval"] as? Number)?.toDouble()
            return HistogramAgg(agg.field, interval ?: agg.interval, agg.offset, agg.key, agg.missing, agg.extensions)
        }
    }

}