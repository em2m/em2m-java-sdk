package io.em2m.geo.geojson

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.geo.feature.Feature
import org.junit.Assert
import org.junit.Test
import org.locationtech.jts.geom.*
import org.skyscreamer.jsonassert.JSONAssert

class GeoJsonModuleTest : Assert() {

    private val factory = GeometryFactory()
    private val mapper = ObjectMapper().registerModule(GeoJsonModule())

    @Test
    fun testSerializePoint() {
        val point = factory.createPoint(Coordinate(-78.0, 39.0))
        val json = mapper.writeValueAsString(point)
        assertEquals("{\"type\":\"Point\",\"coordinates\":[-78.0,39.0]}", json)
        val p2 = mapper.readValue(json, Point::class.java)
        assertEquals(point, p2)
        assertTrue(point.coordinate.equals3D(p2.coordinate))
    }

    @Test
    fun testSimpleFeature() {
        val point = factory.createPoint(Coordinate(-78.0, 39.0))
        val feature = Feature()
        feature.geometry = point
        feature.properties["title"] = "Simple Point Feature"
        val json = mapper.writeValueAsString(feature)
        assertNotNull(json)
    }

    @Test
    fun testDeserializePoint() {
        val json = "{\"point\": {\"type\":\"Point\",\"coordinates\":[-78.0,39.0]}}"
        val hasGeometry = mapper.readValue(json, HasGeometry::class.java)
        assertNotNull(hasGeometry)
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    internal class HasGeometry {
        var point: Point? = null
    }

    @Test
    fun testPoint2D() {
        val expected = "{\"type\": \"Point\", \"coordinates\": [102.0, 0.5]}"
        val g = mapper.readValue(expected, Geometry::class.java)
        assertNotNull(g)
        assertTrue(g is Point)
        val p = g as Point
        assertEquals(102.0, p.coordinate.x, MM_PRECISION)
        assertEquals(0.5, p.coordinate.y, MM_PRECISION)

        val json = mapper.writeValueAsString(p)
        JSONAssert.assertEquals(expected, json, true)
    }

    @Test
    fun testLineString() {
        val expected = ("{\"type\": \"LineString\","
                + "          \"coordinates\": ["
                + "            [102.0, 0.0], [103.0, 1.0], [104.0, 2.0], [105.0, 3.0]"
                + "            ]"
                + "          }")
        val g = mapper.readValue(expected, Geometry::class.java)
        assertNotNull(g)
        assertTrue(g is LineString)
        val ls = g as LineString
        assertEquals(4, ls.coordinates.size.toLong())
        assertEquals(102.0, ls.getCoordinateN(0).x, MM_PRECISION)
        assertEquals(0.0, ls.getCoordinateN(0).y, MM_PRECISION)
        assertEquals(103.0, ls.getCoordinateN(1).x, MM_PRECISION)
        assertEquals(1.0, ls.getCoordinateN(1).y, MM_PRECISION)
        assertEquals(104.0, ls.getCoordinateN(2).x, MM_PRECISION)
        assertEquals(2.0, ls.getCoordinateN(2).y, MM_PRECISION)
        assertEquals(105.0, ls.getCoordinateN(3).x, MM_PRECISION)
        assertEquals(3.0, ls.getCoordinateN(3).y, MM_PRECISION)

        val json = mapper.writeValueAsString(ls)
        JSONAssert.assertEquals(expected, json, true)
    }

    @Test
    fun testPolygon() {
        val expected = ("{\"type\": \"Polygon\","
                + "           \"coordinates\": ["
                + "             [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0],"
                + "               [100.0, 1.0], [100.0, 0.0] ],"
                + "             [ [100.25, 0.25], [100.75, 0.25], [100.75, 0.75],"
                + "               [100.25, 0.75], [100.25, 0.25] ]"
                + "             ]"
                + "         }")
        val g = mapper.readValue(expected, Geometry::class.java)
        assertNotNull(g)
        assertTrue(g is Polygon)
        val p = g as Polygon
        assertEquals(100.0, p.exteriorRing.getCoordinateN(0).x, MM_PRECISION)
        assertEquals(1, p.numInteriorRing.toLong())
        assertEquals(100.25, p.getInteriorRingN(0).getCoordinateN(0).x, MM_PRECISION)

        val json = mapper.writeValueAsString(p)
        JSONAssert.assertEquals(expected, json, true)
    }

    @Test
    fun testMultiPoint() {
        val expected = ("{\"type\": \"MultiPoint\", "
                + "    \"coordinates\": ["
                + "        [10, 40], [40, 30], [20, 20], [30, 10]"
                + "    ]"
                + "}")
        val g = mapper.readValue(expected, Geometry::class.java)
        assertNotNull(g)
        assertTrue(g is MultiPoint)
        val mp = g as MultiPoint
        assertEquals(4, mp.numGeometries.toLong())
        assertEquals(10.0, mp.getGeometryN(0).coordinate.x, MM_PRECISION)
        assertEquals(40.0, mp.getGeometryN(0).coordinate.y, MM_PRECISION)
        assertEquals(40.0, mp.getGeometryN(1).coordinate.x, MM_PRECISION)
        assertEquals(30.0, mp.getGeometryN(1).coordinate.y, MM_PRECISION)
        assertEquals(20.0, mp.getGeometryN(2).coordinate.x, MM_PRECISION)
        assertEquals(20.0, mp.getGeometryN(2).coordinate.y, MM_PRECISION)
        assertEquals(30.0, mp.getGeometryN(3).coordinate.x, MM_PRECISION)
        assertEquals(10.0, mp.getGeometryN(3).coordinate.y, MM_PRECISION)

        val json = mapper.writeValueAsString(mp)
        JSONAssert.assertEquals(expected, json, true)
    }

    @Test
    fun testMultiLineString() {
        val expected = ("{\"type\": \"MultiLineString\", "
                + "    \"coordinates\": ["
                + "        [[10, 10], [20, 20], [10, 40]], "
                + "        [[40, 40], [30, 30], [40, 20], [30, 10]]"
                + "    ]"
                + "}")
        val g = mapper.readValue(expected, Geometry::class.java)
        assertNotNull(g)
        assertTrue(g is MultiLineString)
        val mls = g as MultiLineString
        assertEquals(2, mls.numGeometries.toLong())
        assertTrue(mls.getGeometryN(0) is LineString)
        val ls1 = mls.getGeometryN(0) as LineString
        assertEquals(10.0, ls1.coordinate.x, MM_PRECISION)
        assertTrue(mls.getGeometryN(1) is LineString)
        val ls2 = mls.getGeometryN(1) as LineString
        assertEquals(40.0, ls2.coordinate.x, MM_PRECISION)

        val json = mapper.writeValueAsString(mls)
        JSONAssert.assertEquals(expected, json, true)
    }

    @Test
    fun testMultiPolygon() {
        val expected = ("{\"type\": \"MultiPolygon\","
                + "    \"coordinates\": ["
                + "        ["
                + "          ["
                + "            [101.2, 1.2], [101.8, 1.2], [101.8, 1.8], [101.2, 1.8], [101.2, 1.2]"
                + "          ],"
                + "          ["
                + "            [101.2, 1.2], [101.3, 1.2], [101.3, 1.3], [101.2, 1.3], [101.2, 1.2]"
                + "          ],"
                + "          ["
                + "            [101.6, 1.4], [101.7, 1.4], [101.7, 1.5], [101.6, 1.5], [101.6, 1.4]"
                + "          ],"
                + "          ["
                + "            [101.5, 1.6], [101.6, 1.6], [101.6, 1.7], [101.5, 1.7], [101.5, 1.6]"
                + "          ]"
                + "        ],"
                + "        ["
                + "          ["
                + "            [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]"
                + "          ],"
                + "          ["
                + "            [100.35, 0.35], [100.65, 0.35], [100.65, 0.65], [100.35, 0.65], [100.35, 0.35]"
                + "          ]"
                + "        ]"
                + "      ]"
                + "  }")

        val g = mapper.readValue(expected, Geometry::class.java)
        assertNotNull(g)
        assertTrue(g is MultiPolygon)
        val mp = g as MultiPolygon
        assertEquals(2, mp.numGeometries.toLong())
        assertTrue(mp.getGeometryN(0) is Polygon)
        val p1 = mp.getGeometryN(0) as Polygon
        assertEquals(101.2, p1.exteriorRing.getCoordinateN(0).x, MM_PRECISION)
        assertEquals(3, p1.numInteriorRing.toLong())
        assertTrue(mp.getGeometryN(1) is Polygon)
        val p2 = mp.getGeometryN(1) as Polygon
        assertEquals(100.0, p2.exteriorRing.getCoordinateN(0).x, MM_PRECISION)
        assertEquals(1, p2.numInteriorRing.toLong())

        val json = mapper.writeValueAsString(mp)
        JSONAssert.assertEquals(expected, json, true)
    }

    @Test
    fun testGeometryCollection() {
        val expected = ("{\"type\": \"GeometryCollection\","
                + "    \"geometries\": ["
                + "        {"
                + "            \"type\": \"Point\","
                + "            \"coordinates\": ["
                + "                -80.66080570220947,"
                + "                35.04939206472683"
                + "            ]"
                + "        },"
                + "        {"
                + "            \"type\": \"Polygon\","
                + "            \"coordinates\": ["
                + "                ["
                + "                    ["
                + "                        -80.66458225250244,"
                + "                        35.04496519190309"
                + "                    ],"
                + "                    ["
                + "                        -80.66344499588013,"
                + "                        35.04603679820616"
                + "                    ],"
                + "                    ["
                + "                        -80.66258668899536,"
                + "                        35.045580049697556"
                + "                    ],"
                + "                    ["
                + "                        -80.66387414932251,"
                + "                        35.044280059194946"
                + "                    ],"
                + "                    ["
                + "                        -80.66458225250244,"
                + "                        35.04496519190309"
                + "                    ]"
                + "                ]"
                + "            ]"
                + "        },"
                + "        {"
                + "            \"type\": \"LineString\","
                + "            \"coordinates\": ["
                + "                ["
                + "                    -80.66237211227417,"
                + "                    35.05950973022538"
                + "                ],"
                + "                ["
                + "                    -80.66303730010986,"
                + "                    35.043717894732545"
                + "                ]"
                + "            ]"
                + "        }"
                + "    ]"
                + "}")

        val g = mapper.readValue(expected, Geometry::class.java)
        assertNotNull(g)
        assertTrue(g is GeometryCollection)
        val gc = g as GeometryCollection
        assertEquals(3, gc.numGeometries.toLong())
        assertTrue(gc.getGeometryN(0) is Point)
        assertTrue(gc.getGeometryN(1) is Polygon)
        assertTrue(gc.getGeometryN(2) is LineString)
    }

    companion object {

        private val MM_PRECISION = 0.000000001
    }
}
