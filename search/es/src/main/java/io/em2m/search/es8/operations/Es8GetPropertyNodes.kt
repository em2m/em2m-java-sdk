package io.em2m.search.es8.operations

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.utils.jackson.JacksonPropertyNode
import io.em2m.utils.jackson.JacksonTraversal
import io.em2m.search.es8.getBoxedEs8Type
import io.em2m.search.es8.getPrimitiveEs8Type
import io.em2m.simplex.evalPath
import io.em2m.utils.coerce
import io.em2m.utils.coerceNonNull
import java.util.*

fun es8GetPropertyNodes(obj: Any,
                        objectMapper: ObjectMapper = jacksonObjectMapper(),
                        ignorePaths: Set<String> = emptySet()): Map<String, JacksonPropertyNode> {
    val stack: Deque<JacksonTraversal> = ArrayDeque()

    val clazz = obj as? Class<*> ?: obj.javaClass
    val type = objectMapper.typeFactory.constructType(clazz)
    val beanDescription = objectMapper.serializationConfig.introspect(type)
    beanDescription.findProperties().forEach { property ->
        stack.push(JacksonTraversal(property.name, property.primaryType))
    }

    val propertiesMap = mutableMapOf<String, Map<String, Any>>()

    // could also use recursion for this, but DFS iteration is better
    while (stack.isNotEmpty()) {
        val (path, javaType) = stack.pop()
        if (path in ignorePaths) continue

        val typeDefinition: Map<String, Any> = when {
            javaType.rawClass.isPrimitive -> {
                mapOf("esType" to getPrimitiveEs8Type(javaType.rawClass), "javaType" to javaType)
            }
            javaType.rawClass == String::class.java -> {
                mapOf("esType" to "keyword", "javaType" to javaType)
            }
            javaType.rawClass.name.startsWith("java.time") || javaType.rawClass == Date::class.java -> {
                mapOf("esType" to "date", "javaType" to javaType)
            }
            javaType.isMapLikeType -> {
                // val keyType = javaType.keyType
                val valueType = javaType.contentType
                if (valueType != null && valueType.rawClass != Any::class.java) {
                    val elementBeanDesc = objectMapper.serializationConfig.introspect(valueType)
                    for (prop in elementBeanDesc.findProperties()) {
                        stack.push(JacksonTraversal("$path.${prop.name}", prop.primaryType))
                    }
                } else {
                    // look at actual elements of the object to infer types
                    val asMap = obj.coerce<Map<String,Any?>>().evalPath(path).coerceNonNull(mutableMapOf<String, Any?>())
                    for ((name, value) in asMap.entries) {
                        val valueClazz = value?.javaClass ?: continue
                        val valueType = objectMapper.typeFactory.constructType(valueClazz)
                        stack.push(JacksonTraversal("$path.$name", valueType))
                    }
                }
                mapOf("esType" to "object", "javaType" to javaType)
            }
            javaType.isCollectionLikeType -> {
                val elementType = javaType.contentType
                // allow for lists of strings, ints, doubles etc. complex lists need more investigation
                if (elementType != null) {

                    // I guess this would assume the representative class for the collection is
                    // the polymorphic super class
                    val primitive = getPrimitiveEs8Type(elementType.rawClass)
                    val boxed = getBoxedEs8Type(elementType.rawClass)
                    val innerType = setOf(primitive,boxed).firstOrNull { it != "object" } ?: "object"

                    if (innerType == "object") {
                        val elementBeanDesc = objectMapper.serializationConfig.introspect(elementType)
                        for (prop in elementBeanDesc.findProperties()) {
                            stack.push(JacksonTraversal("$path.${prop.name}", prop.primaryType))
                        }
                        mapOf("esType" to "object", "javaType" to javaType)
                    } else {
                        // convert Collection<String> to a keyword of strings
                        mapOf("esType" to "keyword", "javaType" to javaType)
                    }
                } else {
                    mapOf("esType" to "object", "javaType" to javaType)
                }
            }
            javaType.rawClass.name.startsWith("java.lang") || javaType.rawClass.isEnum -> {
                mapOf("esType" to getBoxedEs8Type(javaType.rawClass), "javaType" to javaType)
            }
            else -> {
                val nestedBeanDesc = objectMapper.serializationConfig.introspect(javaType)
                for (prop in nestedBeanDesc.findProperties()) {
                    stack.push(JacksonTraversal("$path.${prop.name}", prop.primaryType))
                }
                mapOf("esType" to "object", "javaType" to javaType)
            }
        }
        propertiesMap[path] = typeDefinition
    }

    // use linkedhashmap to preserve insertion order, mutable map iirc uses a standard hashmap
    val nodeMap = LinkedHashMap<String, JacksonPropertyNode>()
    propertiesMap.entries.sortedBy{ (path, _) -> path.length }.forEach { (path, props) ->
        if (path in ignorePaths) return@forEach // a bit overkill but wouldn't hurt
        val parent: JacksonPropertyNode? = if ("." in path) {
            val parentReference = path.substringBeforeLast('.')
            nodeMap[parentReference]
        } else {
            null
        }
        val type = props["esType"]?.toString() ?: "object"
        val javaType = props["javaType"].coerce<JavaType>()
        val currNode = JacksonPropertyNode(path = path, esType = type, javaType = javaType, parent)
        nodeMap[path] = currNode
        parent?.let { parent.children.add(currNode) }
    }

    return nodeMap
}
