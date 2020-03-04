package io.em2m.geo.feature

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Feature {
    var id: String? = null
    var type = "Feature"
    var bbox: Envelope? = null
    var crs: CRS? = null
    var geometry: Geometry? = null
    var properties: MutableMap<String, Any> = HashMap()

    fun geometry(coordinate: Coordinate) {
        this.geometry = factory.createPoint(coordinate)
    }

    operator fun get(key: String): Any? {
        return properties[key]
    }
    
    companion object {

        private val factory = GeometryFactory()
    }

}
