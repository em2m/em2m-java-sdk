package io.em2m.utils.jackson

import com.fasterxml.jackson.databind.JavaType

data class JacksonPropertyNode(val path: String,
                val esType: String = "object",
                val javaType: JavaType? = null,
                var parent: JacksonPropertyNode? = null,
                val children: MutableSet<JacksonPropertyNode> = mutableSetOf()) {
    val name: String
        get() = path.substringAfterLast('.')

    override fun toString(): String {
        return """
            JacksonPropertyNode{
                esType=$esType,
                javaType=$javaType,
                parent.name=${parent?.name},
                children=$children}
            }
        """.trimIndent()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is JacksonPropertyNode) return false
        if (this.esType != other.esType) return false
        if (this.javaType != other.javaType) return false
        if (this.path != other.path) return false
        // simple equals
        return true
    }

    override fun hashCode(): Int {
        return listOfNotNull(esType, path, javaType).hashCode()
    }

}
