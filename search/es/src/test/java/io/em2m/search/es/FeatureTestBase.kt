package io.em2m.search.es

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.em2m.geo.feature.Feature
import io.em2m.geo.feature.FeatureCollection
import io.em2m.geo.feature.FeatureCollectionHandler
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.geo.geojson.GeoJsonParser
import io.em2m.search.core.model.FnIdMapper
import org.junit.Assert
import org.junit.Before
import java.io.File
import java.io.FileInputStream

abstract class FeatureTestBase : Assert() {

    @Before
    open fun before() {
        try {
            esClient.deleteIndex("features")
        } catch (e: Exception) {
        }
        flush()
        esClient.createIndex("features")
        flush()
        esClient.putMapping("features", "doc", mapping)
        flush()
        for (feature in earthquakes().features) {
            esClient.put("features", "doc", feature.id!!, feature)
        }
        flush()
    }

    private fun flush() {
        esClient.flush()
        // nodeClient.admin().indices().prepareRefresh().execute().actionGet();
    }

    companion object {

        private var mapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val idMapper = FnIdMapper<Feature>("id", { it.id!! }, { f, id -> f.id = id; f })

        fun earthquakes(): FeatureCollection {
            val handler = FeatureCollectionHandler()
            val parser = GeoJsonParser()
            parser.handler(handler)
            parser.parse(FileInputStream("src/test/resources/earthquakes_2.5_day.geojson"))
            val result = handler.collection
            assertEquals(46, result.features.size)
            return result
        }

        const val es6 = false
        private val mappingPath = if (es6) {
            "src/test/resources/mapping_es6.json"
        } else {
            "src/test/resources/mapping_es2.json"
        }

        const val type = "doc"
        const val index = "features"

        val mapping = mapper.readTree(File(mappingPath)) as ObjectNode

        val esClient: EsApi = Feign.builder()
                .encoder(TextPlainEncoder(JacksonEncoder(mapper)))
                .decoder(JacksonDecoder(mapper))
                .logger(Slf4jLogger())
                .logLevel(feign.Logger.Level.FULL)
                .target(EsApi::class.java, "http://localhost:9200")

    }

}