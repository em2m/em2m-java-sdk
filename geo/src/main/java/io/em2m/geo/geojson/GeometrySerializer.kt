package io.em2m.geo.geojson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.locationtech.jts.geom.*

import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode

class GeometrySerializer(private val precision: Int? = null) : JsonSerializer<Geometry>() {

    @Throws(IOException::class)
    override fun serialize(value: Geometry, generator: JsonGenerator, provider: SerializerProvider) {
        writeGeometry(value, generator)
    }

    @Throws(IOException::class)
    internal fun writeGeometry(geom: Geometry, gen: JsonGenerator) {
        when (geom) {
            is Point -> write(geom, gen)
            is MultiPoint -> write(geom, gen)
            is LineString -> write(geom, gen)
            is MultiLineString -> write(geom, gen)
            is Polygon -> write(geom, gen)
            is MultiPolygon -> write(geom, gen)
            is GeometryCollection -> write(geom, gen)
            else -> throw RuntimeException("Unsupported Geometry type")
        }
    }

    @Throws(IOException::class)
    internal fun write(point: Point, gen: JsonGenerator) {
        gen.writeStartObject()
        gen.writeStringField("type", "Point")
        gen.writeFieldName("coordinates")
        writeCoordinate(point.coordinate, gen)
        gen.writeEndObject()
    }

    @Throws(IOException::class)
    internal fun write(points: MultiPoint, gen: JsonGenerator) {
        gen.writeStartObject()
        gen.writeStringField("type", "MultiPoint")
        gen.writeFieldName("coordinates")
        writeCoordinates(points.coordinates, gen)
        gen.writeEndObject()
    }

    @Throws(IOException::class)
    internal fun write(geom: LineString, gen: JsonGenerator) {
        gen.writeStartObject()
        gen.writeStringField("type", "LineString")
        gen.writeFieldName("coordinates")
        writeCoordinates(geom.coordinates, gen)
        gen.writeEndObject()
    }

    @Throws(IOException::class)
    internal fun write(geom: MultiLineString, gen: JsonGenerator) {
        gen.writeStartObject()
        gen.writeStringField("type", "MultiLineString")
        gen.writeFieldName("coordinates")
        gen.writeStartArray()
        for (i in 0 until geom.numGeometries) {
            writeCoordinates(geom.getGeometryN(i).coordinates, gen)
        }
        gen.writeEndArray()
        gen.writeEndObject()
    }

    @Throws(IOException::class)
    internal fun write(coll: GeometryCollection, gen: JsonGenerator) {
        gen.writeStartObject()
        gen.writeStringField("type", "GeometryCollection")
        gen.writeArrayFieldStart("geometries")
        for (i in 0 until coll.numGeometries) {
            writeGeometry(coll.getGeometryN(i), gen)
        }
        gen.writeEndArray()
        gen.writeEndObject()
    }

    @Throws(IOException::class)
    internal fun write(geom: Polygon, gen: JsonGenerator) {
        gen.writeStartObject()
        gen.writeStringField("type", "Polygon")
        gen.writeFieldName("coordinates")
        gen.writeStartArray()
        writeCoordinates(geom.exteriorRing.coordinates, gen)
        for (i in 0 until geom.numInteriorRing) {
            writeCoordinates(geom.getInteriorRingN(i).coordinates, gen)
        }
        gen.writeEndArray()
        gen.writeEndObject()
    }

    @Throws(IOException::class)
    internal fun write(geom: MultiPolygon, gen: JsonGenerator) {
        gen.writeStartObject()
        gen.writeStringField("type", "MultiPolygon")
        gen.writeFieldName("coordinates")
        gen.writeStartArray()
        for (i in 0 until geom.numGeometries) {
            val p = geom.getGeometryN(i) as Polygon
            gen.writeStartArray()
            writeCoordinates(p.exteriorRing.coordinates, gen)
            for (j in 0 until p.numInteriorRing) {
                writeCoordinates(p.getInteriorRingN(j).coordinates, gen)
            }
            gen.writeEndArray()
        }
        gen.writeEndArray()
        gen.writeEndObject()
    }

    @Throws(IOException::class)
    internal fun writeCoordinate(coordinate: Coordinate, gen: JsonGenerator) {
        gen.writeStartArray()
        writeNumber(coordinate.x, gen)
        writeNumber(coordinate.y, gen)
        gen.writeEndArray()
    }

    @Throws(IOException::class)
    internal fun writeNumber(number: Double, gen: JsonGenerator) {
        if (precision != null) {
            gen.writeNumber(BigDecimal(number).setScale(precision, RoundingMode.HALF_UP))
        } else {
            gen.writeNumber(number)
        }
    }

    @Throws(IOException::class)
    internal fun writeCoordinates(coordinates: Array<Coordinate>, gen: JsonGenerator) {
        gen.writeStartArray()
        for (coord in coordinates) {
            writeCoordinate(coord, gen)
        }
        gen.writeEndArray()
    }
}
