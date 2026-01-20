package io.em2m.search.es8.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import io.em2m.utils.jackson.getOrNull
import io.em2m.utils.jackson.getStringOrNull
import io.em2m.search.es8.models.index.Es8MappingProperty

class Es8MappingPropertyDeserializer : JsonDeserializer<Es8MappingProperty>() {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Es8MappingProperty? {
        if (p == null) return null
        val root: JsonNode = p.codec.readTree(p)

        val type = root.getStringOrNull("type")
        val properties = root.getOrNull("properties")

        val innerProperties = mutableMapOf<String, Es8MappingProperty>()

        properties?.fieldNames()?.forEach { fieldName ->
            val obj = properties.get(fieldName) ?: return@forEach
            innerProperties[fieldName] = p.codec.treeToValue(obj, Es8MappingProperty::class.java)
        }

        val mappingProperty = Es8MappingProperty(type=type, properties=innerProperties)
        innerProperties.values.forEach { mapping ->
            mapping.parent = mappingProperty
        }

        return mappingProperty
    }

}
