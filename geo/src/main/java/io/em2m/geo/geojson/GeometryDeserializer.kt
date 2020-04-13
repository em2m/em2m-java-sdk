package io.em2m.geo.geojson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.locationtech.jts.geom.*

class GeometryDeserializer<T : Geometry?> : JsonDeserializer<T>() {

    private val factory = GeometryFactory()

    override fun deserialize(jsonParser: JsonParser, arg1: DeserializationContext): T? {
        val oc = jsonParser.codec
        val node = oc.readTree<JsonNode>(jsonParser)
        return geometry(node) as? T?
    }

    private fun geometry(node: JsonNode): Geometry? {
        val type = node.get("type").textValue()
        val coordinates = node.get("coordinates") as? ArrayNode

        return when {
            coordinates != null -> when (type) {
                "Point" -> point(coordinates)
                "MultiPoint" -> multiPoint(coordinates)
                "LineString" -> lineString(coordinates)
                "MultiLineString" -> multiLineString(coordinates)
                "Polygon" -> polygon(coordinates)
                "MultiPolygon" -> multiPolygon(coordinates)
                else -> null
            }
            type == "GeometryCollection" -> geometryCollection(node.get("geometries") as ArrayNode)
            else -> null
        }
    }

    private fun point(coordinates: ArrayNode): Geometry {
        val coordinate = toCoordinate(coordinates)
        return factory.createPoint(coordinate)
    }

    private fun multiPoint(nodes: ArrayNode): Geometry {
        val coordinates = toCoordinateArray(nodes)
        return factory.createMultiPointFromCoords(coordinates)
    }

    private fun lineString(nodes: ArrayNode): LineString {
        val coordinates = toCoordinateArray(nodes)
        return factory.createLineString(coordinates)
    }

    private fun multiLineString(nodes: ArrayNode): MultiLineString {
        val lineStrings = arrayOfNulls<LineString>(nodes.size())
        for (i in lineStrings.indices) {
            lineStrings[i] = lineString(nodes.get(i) as ArrayNode)
        }
        return factory.createMultiLineString(lineStrings)
    }

    private fun polygon(nodes: ArrayNode): Polygon {
        val outerRing = toLinearRing(nodes.get(0) as ArrayNode)
        val innerRings = arrayOfNulls<LinearRing>(nodes.size() - 1)
        for (i in innerRings.indices) {
            innerRings[i] = toLinearRing(nodes.get(i + 1) as ArrayNode)
        }
        return factory.createPolygon(outerRing, innerRings)
    }

    private fun multiPolygon(nodes: ArrayNode): MultiPolygon {
        val polygons = arrayOfNulls<Polygon>(nodes.size())
        for (i in polygons.indices) {
            polygons[i] = polygon(nodes.get(i) as ArrayNode)
        }
        return factory.createMultiPolygon(polygons)
    }

    private fun geometryCollection(nodes: ArrayNode): GeometryCollection {
        val geometries = arrayOfNulls<Geometry>(nodes.size())
        for (i in geometries.indices) {
            geometries[i] = geometry(nodes.get(i))
        }
        return factory.createGeometryCollection(geometries)
    }

    private fun toLinearRing(nodes: ArrayNode): LinearRing {
        val coordinates = toCoordinateArray(nodes)
        return factory.createLinearRing(coordinates)
    }

    private fun toCoordinateArray(nodes: ArrayNode): Array<Coordinate> {
        return nodes.map { node ->
            toCoordinate(node as ArrayNode)
        }.toTypedArray()
    }

    private fun toCoordinate(node: ArrayNode): Coordinate {
        var x = 0.0
        var y = 0.0
        var z = java.lang.Double.NaN
        if (node.size() > 1) {
            x = node.get(0).asDouble()
            y = node.get(1).asDouble()
        }
        if (node.size() > 2) {
            z = node.get(2).asDouble()
        }
        return Coordinate(x, y, z)
    }
}
