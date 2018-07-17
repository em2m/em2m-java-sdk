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

    fun getProperties(clazz: Class<*>): Map<String, PropertyDescriptor>? {
        return cache.computeIfAbsent(clazz, loader)
    }

    fun getPropertyDescriptor(clazz: Class<*>?, property: String): PropertyDescriptor? {
        return if (clazz != null) getProperties(clazz)?.get(property) else null
    }

    fun getPropertyValue(obj: Any?, property: String): Any? {
        return try {
            val descriptor = BeanHelper.getPropertyDescriptor(obj?.javaClass, property)
            descriptor?.readMethod?.invoke(obj)
        } catch (ex: Exception) {
            null
        }
    }

}

interface PathPart {
    fun call(obj: Any?): Any?
}

class PropertyPathPart(val property: String) : PathPart {

    override fun call(obj: Any?): Any? {
        val result = when (obj) {
            is Map<*, *> -> obj[property]
            is ObjectNode -> obj.get(property)
            else -> BeanHelper.getPropertyValue(obj, property)
        }
        return if (result is JsonNode) {
            unwrapNode(result)
        } else result
    }

    private fun unwrapNode(node: JsonNode): Any? {
        return when (node) {
            is BinaryNode -> node.binaryValue()
            is BooleanNode -> node.booleanValue()
            is MissingNode -> null
            is NullNode -> null
            is POJONode -> node.pojo
            is TextNode -> node.textValue()
            is NumericNode -> node.numberValue()
        // TODO - Unwrap arrays
            else -> node
        }
    }
}

class PathExpr(val path: String) {

    val parts: List<PathPart> = parse(path)

    fun call(context: Any?): Any? {
        return parts.fold(context) { acc, next -> next.call(acc) }
    }

    fun parse(expr: String): List<PathPart> {
        // split by '.'
        return expr.split(".")
                .map {
                    PropertyPathPart(it)
                }
    }

}

class PathKeyHandler(val simplex: Simplex) : KeyHandler {

    override fun call(key: Key, context: ExprContext): Any? {
        return simplex.getPath(key.name, context)
    }

}

