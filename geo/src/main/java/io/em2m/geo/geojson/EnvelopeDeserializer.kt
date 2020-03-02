package io.em2m.geo.geojson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.locationtech.jts.geom.Envelope
import java.util.*

class EnvelopeDeserializer : JsonDeserializer<Envelope>() {

    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Envelope? {
        // current token is "["
        val values = ArrayList<Double>()
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            val value = jsonParser.doubleValue
            values.add(value)
        }
        var result: Envelope? = null
        if (values.size == 4) {
            result = Envelope(values[0], values[2], values[1], values[3])
        }
        return result
    }
}
