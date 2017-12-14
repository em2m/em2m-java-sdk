package io.em2m.search.es

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.scaleset.geo.Feature
import com.scaleset.geo.FeatureCollection
import com.scaleset.geo.FeatureCollectionHandler
import com.scaleset.geo.geojson.GeoJsonModule
import com.scaleset.geo.geojson.GeoJsonParser
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.em2m.search.core.model.IdMapper

import org.junit.Assert
import org.junit.Before
import java.io.File
import java.io.FileInputStream

abstract class FeatureTestBase : Assert() {

    @Before
    open fun before() {
        try {
            esClient.deleteIndex("features")
        } catch(e: Exception) {
        }
        flush();
        esClient.createIndex("features")
        flush();
        esClient.putMapping("features", "feature", mapping)
        flush();
        for (feature in earthquakes().features) {
            esClient.put("features", "feature", feature.id, feature);
        }
        flush()
    }

    fun flush() {
        esClient.flush()
        // nodeClient.admin().indices().prepareRefresh().execute().actionGet();
    }

    companion object {

        var mapper = jacksonObjectMapper().registerModule(GeoJsonModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        fun earthquakes(): FeatureCollection {
            val handler = FeatureCollectionHandler();
            val parser = GeoJsonParser();
            parser.handler(handler);
            parser.parse(FileInputStream("src/test/resources/earthquakes_2.5_day.geojson"));
            val result = handler.collection;
            Assert.assertEquals(46, result.features.size);
            return result;
        }

        val mapping = mapper.readTree(File("src/test/resources/mapping_es2.json")) as ObjectNode

        val esClient = Feign.builder()
                .encoder(JacksonEncoder(mapper))
                .decoder(JacksonDecoder(mapper))
                .logger(Slf4jLogger())
                .logLevel(feign.Logger.Level.FULL)
                .target(EsApi::class.java, "http://localhost:9200")
    }

    class FeatureMapper : IdMapper<Feature> {

        override val idField = "id"

        override fun getId(obj: Feature): String {
            return obj.id
        }

        override fun setId(obj: Feature, id: String): Feature {
            obj.id = id
            return obj
        }
    }


}