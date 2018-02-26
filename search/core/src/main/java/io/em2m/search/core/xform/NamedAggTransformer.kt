package io.em2m.search.core.xform

import io.em2m.search.core.model.*

class NamedAggTransformer(val namedAggs: Map<String, Agg>) : AggTransformer() {

    override fun transformNamedAgg(agg: NamedAgg): Agg {

        val count = (agg.extensions["size"] as? Number)?.toInt()

        return namedAggs[agg.name]
                ?.let { KeyAggTransformer(agg.key).transform(it) }
                ?: agg.let { ExtensionsTransformer(agg.extensions).transform(it) }
    }

    class ExtensionsTransformer(val ext: Map<String, Any?>) : AggTransformer() {

        override fun transformTermsAgg(agg: TermsAgg): Agg {
            val size = (ext["size"] as? Number)?.toInt()
            return TermsAgg(agg.field, size ?: agg.size, agg.key, agg.sort, agg.format, agg.missing)
        }

        override fun transformDateHistogramAgg(agg: DateHistogramAgg): Agg {
            val interval = (ext["interval"] as? String)
            return DateHistogramAgg(agg.field, agg.format, interval ?: agg.interval, agg.offset, agg.timeZone, agg.key)
        }

        override fun transformHistogramAgg(agg: HistogramAgg): Agg {
            val interval = (ext["interval"] as? Number)?.toDouble()
            return HistogramAgg(agg.field, interval ?: agg.interval, agg.offset, agg.key)
        }
    }

}