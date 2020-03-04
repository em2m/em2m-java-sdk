package io.em2m.geo.geojson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.locationtech.jts.geom.Coordinate
import java.math.BigDecimal
import java.math.RoundingMode

class CoordinateSerializer @JvmOverloads constructor(private val precision: Int? = null) : JsonSerializer<Coordinate>() {

    override fun serialize(coordinate: Coordinate, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
        jsonGenerator.writeStartArray()
        writeNumber(coordinate.x, jsonGenerator)
        writeNumber(coordinate.y, jsonGenerator)
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
