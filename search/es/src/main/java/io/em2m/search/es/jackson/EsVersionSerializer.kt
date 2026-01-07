package io.em2m.search.es.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.em2m.search.es.models.EsVersion

class EsVersionSerializer : JsonSerializer<EsVersion>() {

    override fun serialize(version: EsVersion?, jsonGenerator: JsonGenerator?, provider: SerializerProvider?) {
        if (version == null || jsonGenerator == null) return
        jsonGenerator.writeString(version.toString())
    }

}
