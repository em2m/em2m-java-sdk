package io.em2m.search.es2.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import io.em2m.search.es2.models.Es2GeoPointProperty
import io.em2m.utils.jackson.getBoolean
import io.em2m.utils.jackson.getIntegerOrDefault

@Deprecated("Replace with Es8GeoPoint")
class Es2GeoPointDeserializer: JsonDeserializer<Es2GeoPointProperty>() {

    fun fromJsonNode(root: JsonNode): Es2GeoPointProperty {
        val latLon = root.getBoolean("lat_lon", true)
        val geohash = root.getBoolean("geohash", true)
        val geohashPrefix = root.getBoolean("geohash_prefix", true)
        val geohashPrecision = root.getIntegerOrDefault("geohash_precision", 10)

        return Es2GeoPointProperty(
            latLon = latLon,
            geohash = geohash,
            geohashPrefix = geohashPrefix,
            geohashPrecision = geohashPrecision)
    }

    override fun deserialize(p: JsonParser?, ctx: DeserializationContext?): Es2GeoPointProperty? {
        if (p == null) return null
        val root: JsonNode = p.codec.readTree(p)
        return fromJsonNode(root)
    }


}
