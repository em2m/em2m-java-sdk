package io.em2m.utils

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
        return cache.getOrPut(clazz, { loader.apply(clazz) })
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

    fun putPropertyValue(obj: Any?, property: String, value: Any?) {
        try {
            val descriptor = BeanHelper.getPropertyDescriptor(obj?.javaClass, property)
            descriptor?.writeMethod?.invoke(obj, value)
        } catch (ex: Exception) {
        }
    }

}
