package io.em2m.search.es2.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import io.em2m.obj.jackson.getOrNull
import io.em2m.obj.jackson.getStringOrNull
import io.em2m.search.es2.models.Es2MappingProperty

// JSON --> Es2MappingProperty
@Deprecated("Use Es8")
class Es2Deserializer : JsonDeserializer<Es2MappingProperty>() {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Es2MappingProperty? {
        if (p == null) return null
        val root: JsonNode = p.codec.readTree(p)

        val type = root.getStringOrNull("type")
        val index = root.getStringOrNull("index")
        val properties = root.getOrNull("properties")
        val format = root.getStringOrNull("format")

        val innerProperties = mutableMapOf<String, Es2MappingProperty>()

        properties?.fieldNames()?.forEach { fieldName ->
            val obj = properties.get(fieldName) ?: return@forEach
            innerProperties[fieldName] = p.codec.treeToValue(obj, Es2MappingProperty::class.java)
        }

        val mappingProperty = Es2MappingProperty(type=type, index=index, properties=innerProperties, format=format)
        innerProperties.values.forEach { mapping ->
            mapping.parent = mappingProperty
        }

        return mappingProperty
    }

}
