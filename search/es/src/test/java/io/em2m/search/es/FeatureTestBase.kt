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
import io.em2m.simplex.evalPath
import io.em2m.utils.coerce
import io.em2m.utils.coerceNonNull
import org.junit.Assert
import org.junit.Before
import java.io.File
import java.io.FileInputStream
import java.io.IOException

abstract class FeatureTestBase : Assert() {

    @Before
    open fun before() {
        try {
            esClient.deleteIndex("features")
        } catch (e: Exception) {
            // e.printStackTrace()
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
        esClient.createIndex("creatures")
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

        val esClient: EsApi = Feign.builder()
            .encoder(TextPlainEncoder(JacksonEncoder(mapper)))
            .decoder(JacksonDecoder(mapper))
            .logger(Slf4jLogger())
            .logLevel(feign.Logger.Level.FULL)
            .target(EsApi::class.java, "http://localhost:9200")

        val featureResourceDirectory = File(FeatureTestBase::class.java.getResource("FeatureTestBase")!!.path)
        val es2Directory: File = File(featureResourceDirectory, "es2")
        val es8Directory: File = File(featureResourceDirectory, "es8")

        class EsAliasDirectory(val alias: String, val indexDirectory: List<File>)

        // TODO: Move to high level
        open class EsIndexDefinition(open val indexName: String, open val mapping: ObjectNode)

        class EsIndexBundle(val alias: String, val esMappings: List<EsIndexDefinition>)

        // TODO: Move to migration directories
        class Es2IndexDefinition(override val indexName: String, override val mapping: ObjectNode, val types: List<String>) : EsIndexDefinition(indexName, mapping)

        class Es2IndexBundle(val alias: String, val mappings: List<Es2IndexDefinition>)

        fun loadEs2Indices(esDirectory: File, alias: String): Es2IndexBundle {
            val fromIndexDirectories = File(esDirectory, alias).listFiles()?.filter { it.isDirectory } ?: emptyList()

            val esMappings = fromIndexDirectories.map { file ->
                val indexName = file.name
                val mappingFile = File(file, "mapping.json")
                val mapping = mapper.readTree(mappingFile) as ObjectNode
                val types = mapping.get("mappings").fieldNames().asSequence().toList()
                Es2IndexDefinition(indexName, mapping, types)
            }

            return Es2IndexBundle(alias, esMappings)
        }

        fun loadIndices(esDirectory: File, alias: String): EsIndexBundle {
            val fromIndexDirectories = File(esDirectory, alias).listFiles()?.filter { it.isDirectory } ?: emptyList()

            val esMappings = fromIndexDirectories.map { file ->
                val indexName = file.name
                val mappingFile = File(file, "mapping.json")
                val mapping = mapper.readTree(mappingFile) as ObjectNode
                EsIndexDefinition(indexName, mapping)
            }

            return EsIndexBundle(alias, esMappings)
        }


        val es6 = esClient.getStatus().version.major == 6
        private val mappingPath = if (es6) {
            "src/test/resources/mapping_es6.json"
        } else {
            "src/test/resources/mapping_es2.json"
        }

        const val type = "doc"
        const val index = "features"

        val mapping = mapper.readTree(File(mappingPath)) as ObjectNode

    }

}
