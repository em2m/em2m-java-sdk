package io.em2m.geo.geojson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.locationtech.jts.geom.Coordinate
import java.util.*

class CoordinateDeserializer : JsonDeserializer<Coordinate>() {

    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Coordinate? {
        // current token is "["
        val values = ArrayList<Double>()
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            val value = jsonParser.doubleValue
            values.add(value)
        }
        var result: Coordinate? = null
        if (values.size == 2) {
            result = Coordinate(values[0], values[1])
        } else if (values.size == 3) {
            result = Coordinate(values[0], values[1], values[2])
        }
        return result

    }
}
