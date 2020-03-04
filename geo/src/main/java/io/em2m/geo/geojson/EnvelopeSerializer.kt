package io.em2m.geo.geojson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.locationtech.jts.geom.Envelope
import java.math.BigDecimal
import java.math.RoundingMode

class EnvelopeSerializer @JvmOverloads constructor(private val precision: Int? = null) : JsonSerializer<Envelope>() {

    override fun serialize(envelope: Envelope, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
        jsonGenerator.writeStartArray()
        writeNumber(envelope.minX, jsonGenerator)
        writeNumber(envelope.minY, jsonGenerator)
        writeNumber(envelope.maxX, jsonGenerator)
        writeNumber(envelope.maxY, jsonGenerator)
        jsonGenerator.writeEndArray()
    }

    private fun writeNumber(number: Double, gen: JsonGenerator) {
        if (precision != null) {
            gen.writeNumber(BigDecimal(number).setScale(precision, RoundingMode.HALF_UP))
        } else {
            gen.writeNumber(number)
        }
    }

}
