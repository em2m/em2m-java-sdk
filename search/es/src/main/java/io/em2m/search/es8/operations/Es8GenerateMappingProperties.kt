package io.em2m.search.es8.operations

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es2.models.Es2Mapping
import io.em2m.search.es2.models.Es2MappingProperty
import io.em2m.search.es8.models.index.Es8Mapping
import io.em2m.search.es8.models.index.Es8MappingProperty
import io.em2m.search.migrate.migrateEs2ToEs8

private data class Node(val path: String,
                        val esType: String = "object",
                        val javaType: JavaType? = null,
                        var parent: Node? = null,
                        val children: MutableSet<Node> = mutableSetOf()) {
    val name: String
        get() = path.substringAfterLast('.')
}

@Deprecated("When using a class instead of an object, there isn't information on the key-value pairs of a nested Map",
    ReplaceWith("generateEs8MappingProperties(clazz as Any, objectMapper=jacksonObjectMapper())",
        "com.fasterxml.jackson.module.kotlin.jacksonObjectMapper"))
fun es8GenerateMappingProperties(clazz: Class<*>,
                                 objectMapper: ObjectMapper = jacksonObjectMapper(),
                                 ignorePaths: Set<String> = emptySet()) : Es8MappingProperty {
    return es8GenerateMappingProperties(clazz as Any, objectMapper, ignorePaths)
}

// This would theoretically allow us to generate schema definitions from protocol objects
fun es8GenerateMappingProperties(obj: Any,
                                 objectMapper: ObjectMapper = jacksonObjectMapper(),
                                 ignorePaths: Set<String> = emptySet()): Es8MappingProperty {
    // empty case
    if (obj is Collection<*> && obj.isEmpty()) {
        return Es8MappingProperty()
    }

    // identity
    if (obj is Es8Mapping) return obj.properties
    if (obj is Collection<*> && obj.any { it is Es8MappingProperty }) {
        return es8MergeProperties(mappings = obj.filterIsInstance<Es8MappingProperty>().toTypedArray())
    }

    // migrate old mappings and properties
    if (obj is Es2Mapping) return migrateEs2ToEs8(obj).properties
    if (obj is Collection<*> && obj.any { it is Es2MappingProperty }) {
        return es8MergeProperties(mappings = obj.filterIsInstance<Es2MappingProperty>().map(::migrateEs2ToEs8).toTypedArray())
    }

    val nodeMap = es8GetPropertyNodes(obj, objectMapper, ignorePaths)

    // put all properties into a tree
    val rootProperties = mutableSetOf<Es8MappingProperty>()
    val rootPropertyMap = mutableMapOf<String, Es8MappingProperty>()
    val workingMemoryPropertyMap = LinkedHashMap<String, Es8MappingProperty>()
    nodeMap.mapValues { (_, node) ->
        val parentProperty = node.parent?.path?.let { parentPath -> workingMemoryPropertyMap[parentPath] }
        val property = Es8MappingProperty(node.esType)
        parentProperty?.let {
            if (parentProperty.properties == null) {
                parentProperty.properties = mutableMapOf()
            }
            parentProperty.properties?.put(node.name, property)
        }
        workingMemoryPropertyMap[node.path] = property
        if (parentProperty == null) {
            rootProperties.add(property)
            rootPropertyMap[node.name] = property
        }
    }

    return Es8MappingProperty(properties = rootPropertyMap)
}
