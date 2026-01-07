package io.em2m.search.es8.models.index

import com.fasterxml.jackson.annotation.JsonInclude
import io.em2m.search.EsPropertyConflictStrategy
import io.em2m.search.es8.models.Es8Dynamic
import io.em2m.search.es8.operations.es8MergeProperties
import io.em2m.simplex.evalPath
import io.em2m.utils.Coerce

@JsonInclude(JsonInclude.Include.NON_EMPTY)
open class Es8MappingProperty(var type: String? = null,
                              var properties: MutableMap<String, Es8MappingProperty>? = null,
                              var parent: Es8MappingProperty? = null,
                              var format: String? = null,
                              var dynamic: Es8Dynamic? = null) {

    inline fun <reified T> Es8MappingProperty?.evalPath(path: String, fallback: T? = null): T? {
        if (this == null) return null
        if (T::class.java == Es8MappingProperty::class.java) {
            return this[path] as? T ?: fallback
        }
        return Coerce.evalPath(path, fallback)
    }

    operator fun get(path: String): Es8MappingProperty? {
        if ("." in path) {
            // evalPath would just be:
            // `a.properties.b.properties.c.properties.d`
            // instead of `a.b.c.d`
            val first = path.substringBefore(".")
            val remaining = path.substringAfter(".")
            val local = properties?.get(first)
            return local?.get(remaining)
        }
        return properties?.get(path)
    }

    operator fun set(path: String, property: Es8MappingProperty) {
        if ("." in path) {
            val first = path.substringBefore(".")
            val remaining = path.substringAfter(".")
            if (this.properties == null) {
                val newProps = mutableMapOf<String, Es8MappingProperty>()
                val local = Es8MappingProperty(type="object", parent=this)
                newProps[first] = local
                local[remaining] = property
                this.properties = newProps
            } else {
                val local = this.properties?.get(first) ?: return
                local[remaining] = property
            }
        } else {
            if (this.properties == null) {
                this.properties = mutableMapOf()
            }
            this.properties?.set(path, property)
        }
    }

    fun removePath(path: String): Es8MappingProperty? {
        if ("." in path) {
            val first = path.substringBefore(".")
            val remaining = path.substringAfter(".")
            val local = properties?.get(first) ?: return null
            return local.removePath(remaining)
        }
        return properties?.remove(path)
    }

    operator fun contains(key: String): Boolean {
        return this[key] != null
    }

//    fun isEmpty(): Boolean {
//        return type.isNullOrBlank() && properties.isNullOrEmpty() && format.isNullOrBlank()
//    }

    fun merge(other: Es8MappingProperty, strategy: EsPropertyConflictStrategy = EsPropertyConflictStrategy.PERMISSIVE): Es8MappingProperty {
        return es8MergeProperties(this, other, strategy = strategy)
    }

    override fun toString(): String {
        return ("""
                Es8MappingProperty {
                    """ + if (type != null) { "type=$type," } else {""} + """
                    """ + if (format != null) { "format=$format," } else {""} + """
                    """ + if (dynamic != null) { "dynamic=$dynamic," } else {""} + """
                    children=""" + (properties?.keys ?: emptySet<Any>()) + """
                }
            """).trimMargin().replace(" ", "").replace("\n", "")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Es8MappingProperty) return false
        if (other.type != this.type) return false
        if (other.format != this.format) return false
        if (other.dynamic != this.dynamic) return false
        return this.properties == other.properties
    }

    override fun hashCode(): Int {
        return listOfNotNull(type, properties, format, dynamic).hashCode()
    }

}
