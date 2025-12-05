package io.em2m.search.es.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import io.em2m.utils.jackson.getStringOrNull
import io.em2m.search.es.models.EsVersion
import kotlin.math.floor

class EsVersionDeserializer : JsonDeserializer<EsVersion>() {

    override fun deserialize(p: JsonParser?, ctx: DeserializationContext?): EsVersion? {
        if (p == null) return null
        val root: JsonNode = p.codec.readTree(p)

        return when (root.nodeType) {
            JsonNodeType.STRING -> {
                val number = if (root.isTextual) {
                    root.asText(null)
                } else {
                    root.getStringOrNull("number")
                }
                number?.let { EsVersion(number) }
            }

            JsonNodeType.NUMBER -> {
                val numericValue = root.numberValue().toDouble()
                when (val major = floor(numericValue).toInt()) {
                    2 -> EsVersion.ES2
                    8 -> EsVersion.ES8
                    else -> {
                        val minor = numericValue - major
                        EsVersion("$major.$minor")
                    }
                }
            }
            else -> {
                val number = root.getStringOrNull("number")
                number?.let { EsVersion(number) }
            }
        }
    }
}
