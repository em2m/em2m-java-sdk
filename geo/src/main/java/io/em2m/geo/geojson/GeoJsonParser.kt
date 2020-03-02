package io.em2m.geo.geojson

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.MappingJsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.geo.feature.AbstractFeatureParser
import io.em2m.geo.feature.Feature
import org.locationtech.jts.geom.Envelope

import java.io.InputStream

class GeoJsonParser : AbstractFeatureParser() {

    private val objectMapper = ObjectMapper().registerModule(GeoJsonModule())

    @Throws(Exception::class)
    override fun parse(`in`: InputStream) {
        begin()
        val f = MappingJsonFactory()
        val jp = f.createParser(`in`)
        var current: JsonToken
        current = jp.nextToken()
        if (current != JsonToken.START_OBJECT) {
            println("Error: root should be object: quiting.")
            return
        }
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            val fieldName = jp.currentName
            // move from field name to field value
            if (fieldName == "features") {
                current = jp.nextToken()
                if (current == JsonToken.START_ARRAY) {
                    // For each of the records in the array
                    while (jp.nextToken() != JsonToken.END_ARRAY) {
                        val feature = objectMapper.readValue(jp, Feature::class.java)
                        handle(feature)
                    }
                } else {
                    println("Error: records should be an array: skipping.")
                    jp.skipChildren()
                }
            } else if (fieldName == "bbox") {
                // advance to '['
                jp.nextToken()
                val bbox = objectMapper.readValue(jp, Envelope::class.java)
                handle(bbox)
            } else {
                if ("type" != fieldName) {
                    println("Unprocessed property: $fieldName")
                }
                jp.skipChildren()
            }
        }
        `in`.close()
        end()
    }

}
