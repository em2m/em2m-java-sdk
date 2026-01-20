package io.em2m.search.es8

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.core.model.GeoHashAgg
import io.em2m.search.es.EsAggs
import io.em2m.search.es.RequestConverter

class Es8RequestConverter(override val objectMapper: ObjectMapper = jacksonObjectMapper()) : RequestConverter() {

    override fun convertGeoHashAgg(result: EsAggs, agg: GeoHashAgg, subAggs: EsAggs?): EsAggs {
        val subAggs = subAggs ?: EsAggs()

        subAggs.agg("centroid", "geo_centroid")
            .field(agg.field)

        result.agg(agg.key, "geotile_grid", subAggs)
            .minDocCount(agg.minDocCount)
            .precision(agg.precision)

        return result
    }

}
