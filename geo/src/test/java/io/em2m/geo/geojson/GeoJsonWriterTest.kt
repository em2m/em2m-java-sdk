package io.em2m.geo.geojson

import io.em2m.geo.feature.FeatureCollection
import io.em2m.geo.feature.FeatureCollectionHandler
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.FileInputStream


class GeoJsonWriterTest : Assert() {

    @Test
    @Throws(Exception::class)
    fun testWriteFeatures() {
        val fc = parseFeatures()
        val writer = GeoJsonWriter()
        val out = ByteArrayOutputStream()
        writer.start(out)
        for (feature in fc.features) {
            writer.feature(feature)
        }
        writer.end()
        val json = out.toString()
        Assert.assertNotNull(json)
    }

    @Throws(Exception::class)
    private fun parseFeatures(): FeatureCollection {
        val handler = FeatureCollectionHandler()
        val parser = GeoJsonParser()
        parser.handler(handler)
        parser.parse(FileInputStream("src/test/resources/features.json"))
        return handler.collection
    }

}
