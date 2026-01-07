package io.em2m.search.es2.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.MAPPING_FILE_NAME
import io.em2m.search.es2.jackson.Es2Deserializer
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = Es2Deserializer::class)
@Deprecated("Use Es8MappingProperty")
open class Es2MappingProperty(val type: String? = null,
                              open val index: String? = null,
                              open val properties: Map<String, Es2MappingProperty>? = null,
                              open var parent: Es2MappingProperty? = null,
                              open var format: String? = null,
                              open var dynamic: Boolean? = null) {

    val children = properties?.values

    override fun toString(): String {
        return ("""
                Es2MappingProperty {
                    """ + if (type != null) { "type=$type," } else { "" } + """
                    """ + if (index != null) { "index=$index," } else { "" } + """
                    """ + if (format != null) { "format=$format," } else { "" } + """
                    """ + if (dynamic != null) { "dynamic=$dynamic," } else { "" } + """
                    children=""" + (properties?.keys ?: emptySet<Any>()) + """
                }
            """).trimMargin().replace(" ", "").replace("\n", "")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Es2MappingProperty) return false
        if (other.type != this.type) return false
        if (other.index != this.index) return false
        if (other.format != this.format) return false
        if (other.dynamic != this.dynamic) return false
        return this.properties == other.properties
    }

    override fun hashCode(): Int {
        return listOfNotNull(type, index, properties, format, dynamic).hashCode()
    }

    companion object {

        fun load(es2File: File,
                 objectMapper: ObjectMapper = jacksonObjectMapper(),
                 indexWhitelist: List<String> = emptyList()): Map<File, Es2MappingProperty> {
            if (!es2File.exists()) return mutableMapOf()
            val useWhitelist = indexWhitelist.isNotEmpty()
            return if (es2File.isFile) {
                // attempt to read file as a json object
                mapOf(es2File to objectMapper.readValue(es2File, Es2MappingProperty::class.java))
            } else {
                es2File.walkTopDown()
                    .filter(File::isFile)
                    .filter { file ->
                        if (!useWhitelist) {
                            true
                        } else {
                            val parent = file.parentFile
                            parent.name in indexWhitelist
                        }
                    }
                    .filter { file -> file.nameWithoutExtension == MAPPING_FILE_NAME && file.extension == "json" }
                    .associateWith { objectMapper.readValue(it, Es2MappingProperty::class.java) }
            }
        }

    }

}
