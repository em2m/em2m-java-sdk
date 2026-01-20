package io.em2m.search.es2.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.em2m.search.es8.models.index.properties.Es8GeoPointProperty

@Deprecated("Replace with Es8GeoPoint")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class Es2GeoPointProperty(@param:JsonProperty("lat_lon") val latLon: Boolean = true,
                          val geohash: Boolean = true,
                          @param:JsonProperty("geohash_prefix") val geohashPrefix: Boolean = true,
                          @param:JsonProperty("geohash_precision") val geohashPrecision: Int = 10)
    : Es2MappingProperty(type = "geo_point") {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Es2GeoPointProperty) return false
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
