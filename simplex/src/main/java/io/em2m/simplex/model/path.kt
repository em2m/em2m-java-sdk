package io.em2m.simplex.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import io.em2m.simplex.Simplex
import java.beans.IntrospectionException
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function


class BeanPropertiesLoader : Function<Class<*>, Map<String, PropertyDescriptor>> {

    override fun apply(t: Class<*>): Map<String, PropertyDescriptor> {
        return try {
            Introspector.getBeanInfo(t)?.propertyDescriptors?.associateBy { it.name } ?: emptyMap()
        } catch (e: IntrospectionException) {
            emptyMap()
        }
    }
}

object BeanHelper {

    private val cache = ConcurrentHashMap<Class<*>, Map<String, PropertyDescriptor>>()

    private val loader = BeanPropertiesLoader()

    private fun getProperties(clazz: Class<*>): Map<String, PropertyDescriptor>? {
        return cache.getOrPut(clazz) { loader.apply(clazz) }
    }

    private fun getPropertyDescriptor(clazz: Class<*>?, property: String): PropertyDescriptor? {
        return if (clazz != null) getProperties(clazz)?.get(property) else null
    }

    fun getPropertyValue(obj: Any?, property: String): Any? {
        return try {
            val descriptor = getPropertyDescriptor(obj?.javaClass, property)
            descriptor?.readMethod?.invoke(obj)
        } catch (ex: Exception) {
            null
        }
    }

    fun putPropertyValue(obj: Any?, property: String, value: Any?) {
        try {
            val descriptor = getPropertyDescriptor(obj?.javaClass, property)
            descriptor?.writeMethod?.invoke(obj, value)
        } catch (ex: Exception) {
        }
    }

}

interface PathPart {
    fun get(obj: Any?): Any?
    fun getOrPut(obj: Any?, fn: (String) -> Any?): Any?
    fun put(obj: Any?, value: Any?)
    fun remove(obj: Any?)
}

class PropertyPathPart(val property: String) : PathPart {

    val index = property.toIntOrNull()

    override fun get(obj: Any?): Any? {

        val result = when (obj) {
            is Map<*, *> -> obj[property]
            is ObjectNode -> obj.get(property)
            else -> {
                if (obj is List<*> && index != null) {
                    obj[index]
                } else if (obj is Array<*> && index != null) {
                    obj[index]
                } else if (obj is Array<*>) {
                    obj.map {
                        get(it)
                    }
                } else if (obj is List<*>) {
                    obj.map {
                        get(it)
                    }
                } else {
                    BeanHelper.getPropertyValue(obj, property)
                }
            }
        }
        return if (result is JsonNode) {
            unwrapNode(result)
        } else result
    }

    override fun getOrPut(obj: Any?, fn: (String) -> Any?): Any? {

        val result = when (obj) {
            is ObjectNode -> {
                val result = obj.get(property)
                if (result is MissingNode || result is NullNode) {
                    obj.set(property, JsonNodeFactory.instance.objectNode())
                } else result
            }

            is MutableMap<*, *> -> {
                (obj as MutableMap<String, Any?>).computeIfAbsent(property, fn)
            }

            else -> {
                if (obj is List<*> && index != null) {
                    obj[index]
                } else if (obj is Array<*> && index != null) {
                    obj[index]
                } else {
                    var result = BeanHelper.getPropertyValue(obj, property)
                    if (result == null) {
                        val value = fn(property)
                        BeanHelper.putPropertyValue(obj, property, value)
                        result = value
                    }
                    result
                }
            }
        }
        return result
    }

    override fun put(obj: Any?, value: Any?) {
        when (obj) {
            is ObjectNode -> obj.putPOJO(property, value)
            is MutableMap<*, *> -> (obj as MutableMap<String, Any?>)[property] = value
            else -> {
                if (obj is MutableList<*> && index != null) {
                    (obj as MutableList<Any?>)[index] = value
                } else if (obj is Array<*> && index != null) {
                    (obj as Array<Any?>)[index] = value
                } else {
                    BeanHelper.putPropertyValue(obj, property, value)
                }
            }
        }
    }

    override fun remove(obj: Any?) {
        when (obj) {
            is MutableMap<*, *> -> (obj as MutableMap<String, Any?>).remove(property)
            is ObjectNode -> obj.remove(property)
            else -> {
                if (obj is MutableList<*> && index != null) {
                    obj.removeAt(index)
                } else if (obj is Array<*> && index != null) {
                    (obj as Array<Any?>)[index] = null
                } else {
                    BeanHelper.putPropertyValue(obj, property, null)
                }
            }
        }
    }

    private fun unwrapNode(node: JsonNode): Any? {
        return when (node) {
            is TextNode -> node.textValue()
            is ArrayNode -> node.map { unwrapNode(it) }
            is ObjectNode -> node.fields().asSequence().map { it.key to unwrapNode(it.value) }.toMap()
            is BooleanNode -> node.booleanValue()
            is NumericNode -> node.numberValue()
            is NullNode -> null
            is POJONode -> node.pojo
            is BinaryNode -> node.binaryValue()
            is MissingNode -> null
            else -> node
        }
    }

}


class PathExpr(val path: String) {

    private val parts: List<PathPart> = parse(path)

    fun call(context: Any?): Any? {
        return parts.fold(context) { acc, next -> next.get(acc) }
    }

    fun parse(expr: String): List<PathPart> {
        // split by '.'
        return expr.split(".")
            .map {
                PropertyPathPart(it)
            }
    }

    fun setValue(context: Any?, value: Any?) {
        val parent = parts.dropLast(1).fold(context) { acc, next ->
            next.getOrPut(acc) { HashMap<String, Any?>() }
        }
        parts.last().put(parent, value)
    }

    fun addValue(context: Any?, value: Any?) {
        val parent = parts.dropLast(1).fold(context) { acc, next ->
            next.getOrPut(acc) { HashMap<String, Any?>() }
        }
        when (val currentValue = parts.last().get(parent)) {

            null -> setValue(context, value)

            is MutableMap<*, *> -> {
                when (value) {
                    is Map<*, *> -> setValue(context, currentValue + value)
                }
            }

            is Collection<*> -> {
                when (value) {
                    is Collection<*> -> setValue(context, currentValue + value)
                    else -> setValue(context, currentValue + value)
                }
            }

            else -> {}
        }
    }

    fun removeValue(context: Any?) {
        val parent = parts.dropLast(1).fold(context) { acc, next ->
            next.get(acc)
        }
        parts.last().remove(parent)
    }

}

class PathKeyHandler(private val prefix: String? = null, private val addSeparator: Boolean = true) : KeyHandler {

    private fun keyNames(key: Key): List<String> {
        return key.name.split(',').map { it.trim() }
    }

    override fun call(key: Key, context: ExprContext): Any? {
        val paths = keyNames(key).map { getPath(it, context) }
        return when {
            paths.size > 1 -> paths
            else -> paths.first()
        }
    }

    private fun getPath(name: String, context: ExprContext): Any? {
        return when {
            prefix == null -> {
                Simplex.simplex.getPath(name, context)
            }

            addSeparator -> {
                Simplex.simplex.getPath("$prefix.$name", context)
            }

            else -> {
                Simplex.simplex.getPath(prefix + name, context)
            }
        }
    }
}

