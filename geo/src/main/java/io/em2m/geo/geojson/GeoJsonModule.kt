package io.em2m.geo.geojson

import com.fasterxml.jackson.databind.module.SimpleModule
import org.locationtech.jts.geom.*

class GeoJsonModule @JvmOverloads constructor(precision: Int? = null) : SimpleModule() {

    init {
        // deserializers - Jackson requires a deserializer for each subclass of geometry
        // val de = GeometryDeserializer()
        addDeserializer(Geometry::class.java, GeometryDeserializer())
        addDeserializer(GeometryCollection::class.java, GeometryDeserializer())
        addDeserializer(Point::class.java, GeometryDeserializer())
        addDeserializer(LinearRing::class.java, GeometryDeserializer())
        addDeserializer(LineString::class.java, GeometryDeserializer())
        addDeserializer(MultiLineString::class.java, GeometryDeserializer())
        addDeserializer(MultiPoint::class.java, GeometryDeserializer())
        addDeserializer(MultiPolygon::class.java, GeometryDeserializer())
        addDeserializer(Polygon::class.java, GeometryDeserializer())
        addDeserializer(Envelope::class.java, EnvelopeDeserializer())
        addDeserializer(Coordinate::class.java, CoordinateDeserializer())

        // serializers
        addSerializer(Geometry::class.java, GeometrySerializer(precision))
        addSerializer(Envelope::class.java, EnvelopeSerializer(precision))
        addSerializer(Coordinate::class.java, CoordinateSerializer(precision))
    }

    companion object {
        // internal var GEO_DE = GeometryDeserializer()
    }

}
