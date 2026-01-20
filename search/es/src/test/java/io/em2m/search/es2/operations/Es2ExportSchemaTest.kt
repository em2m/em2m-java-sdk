package io.em2m.search.es2.operations

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es2.Es2Api
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.es.TextPlainEncoder
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * env variables:
 *
 * es2Url=http://some.es2.url:9200
 * indexWhitelist=index-name-2025-12-13,big-macs-2005-01-15
 *
 * */
@Ignore
class Es2ExportSchemaTest {

    val indexWhitelist: List<String> = run {
        System.getenv()["indexWhitelist"]?.split(",") ?: emptyList()
    }.map(String::trim)

    var es2Url: String = System.getenv()["es2Url"] ?: "http://localhost:9200"

    val mapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val es2Client: Es2Api =  Feign.builder()
        .encoder(TextPlainEncoder(JacksonEncoder(mapper)))
        .decoder(JacksonDecoder(mapper))
        .logger(Slf4jLogger())
        .logLevel(feign.Logger.Level.FULL)
        .target(Es2Api::class.java, es2Url)

    val es2Status = es2Client.getStatus().run {
        assert(version.major == 2) { "Unexpected source version: $version"}
    }

    @Test
    fun `export all es2 mappings`() {
        val exportedSchema = es2Client.exportSchema(indexWhitelist=indexWhitelist)
        println(exportedSchema)
    }

}
