package io.em2m.geo.geojson

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.geo.feature.Feature

import java.io.IOException
import java.io.OutputStream

class GeoJsonWriter {

    private val jsonFactory = JsonFactory()

    private var jg: JsonGenerator? = null
    private val objectMapper = ObjectMapper().registerModule(GeoJsonModule())

    @Throws(IOException::class)
    fun end() {
        jg!!.writeEndArray()
        jg!!.writeEndObject()
        jg!!.close()
    }

    @Throws(IOException::class)
    fun feature(feature: Feature) {
        objectMapper.writeValue(jg!!, feature)
    }

    @Throws(IOException::class)
    fun start(out: OutputStream) {
        jg = jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, true).createGenerator(out,
                JsonEncoding.UTF8)
        jg!!.writeStartObject()
        jg!!.writeFieldName("type")
        jg!!.writeString("FeatureCollection")
        jg!!.writeFieldName("features")
        jg!!.writeStartArray()
    }

}
