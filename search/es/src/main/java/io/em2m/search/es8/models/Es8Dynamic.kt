package io.em2m.search.es8.models

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.JsonNodeType
import io.em2m.search.es8.models.Es8Dynamic.FALSE
import io.em2m.search.es8.models.Es8Dynamic.TRUE

@JsonSerialize(using = Es8DynamicSerializer::class)
@JsonDeserialize(using = Es8DynamicDeserializer::class)
enum class Es8Dynamic {
    TRUE, FALSE, STRICT, RUNTIME;

    override fun toString(): String {
        return super.toString().lowercase()
    }

    companion object {

        fun fromBoolean(bool: Boolean?): Es8Dynamic? = Es8DynamicDeserializer.fromBoolean(bool)

        fun fromString(str: String?): Es8Dynamic? = Es8DynamicDeserializer.fromString(str)

    }

}

class Es8DynamicSerializer() : JsonSerializer<Es8Dynamic>() {

    override fun serialize(dynamic: Es8Dynamic?, jsonGenerator: JsonGenerator?, provider: SerializerProvider?) {
        if (dynamic == null || jsonGenerator == null) return
        jsonGenerator.writeString(dynamic.toString().lowercase())
    }

}

class Es8DynamicDeserializer(): JsonDeserializer<Es8Dynamic>() {

    companion object {

        fun fromBoolean(bool: Boolean?): Es8Dynamic? {
            if (bool == null) return null
            return if (bool) TRUE else FALSE
        }

        fun fromString(str: String?): Es8Dynamic? {
            if (str == null) return null
            return try {
                Es8Dynamic.valueOf(str.uppercase())
            } catch (_ : Exception) {
                null
            }
        }

    }

    override fun deserialize(p: JsonParser?, p1: DeserializationContext?): Es8Dynamic? {
        if (p == null) return null
        return try {
            val root: JsonNode = p.codec.readTree(p)
            when (root.nodeType) {
                JsonNodeType.BOOLEAN -> {
                    fromBoolean(root.booleanValue())
                }
                JsonNodeType.STRING -> {
                    if(root.isTextual) {
                        fromString(root.textValue())
                    } else {
                        FALSE
                    }
                }
                else -> {
                    FALSE
                }
            }

        } catch (_: Exception) {
            FALSE
        }
    }

}
