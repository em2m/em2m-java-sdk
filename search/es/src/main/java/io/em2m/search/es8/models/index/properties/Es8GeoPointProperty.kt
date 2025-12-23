package io.em2m.search.es8.models.index.properties

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.em2m.search.es8.models.index.Es8MappingProperty

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class Es8GeoPointProperty(@param:JsonProperty("lat_lon") val latLon: Boolean = true,
                          val geohash: Boolean = true,
                          @param:JsonProperty("geohash_prefix") val geohashPrefix: Boolean = true,
                          @param:JsonProperty("geohash_precision") val geohashPrecision: Int = 10)
    : Es8MappingProperty(type= "geo_point") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Es8GeoPointProperty) return false
        if (latLon != other.latLon) return false
        if (geohash != other.geohash) return false
        if (geohashPrefix != other.geohashPrefix) return false
        if (geohashPrecision != other.geohashPrecision) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        val superCode = super.hashCode()
        return listOfNotNull(superCode, latLon, geohash, geohashPrefix, geohashPrecision).hashCode()
    }

}
