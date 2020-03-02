package io.em2m.geo.geojson

import io.em2m.geo.feature.FeatureCollectionHandler
import org.junit.Assert
import org.junit.Test
import org.locationtech.jts.geom.LineString

class GeoJsonParserTest : Assert() {

    @Test
    @Throws(Exception::class)
    fun testParseFeatures() {
        val handler = FeatureCollectionHandler()
        val parser = GeoJsonParser()
        parser.handler(handler)
        parser.parse(javaClass.getResourceAsStream("/features.json"))
        val fc = handler.collection
        Assert.assertEquals(4, fc.features.size.toLong())

        // Feature Collection bbox
        val bbox = fc.bbox
        Assert.assertNotNull(bbox)
        Assert.assertEquals(-180.0, bbox!!.minX, 0.00001)
        Assert.assertEquals(-90.0, bbox.minY, 0.00001)
        Assert.assertEquals(180.0, bbox.maxX, 0.00001)
        Assert.assertEquals(90.0, bbox.maxY, 0.00001)

        val f1 = fc.features[1]
        val g = f1.geometry
        Assert.assertTrue(g is LineString)

        val e = f1.bbox
        Assert.assertNotNull(e)
    }

}
