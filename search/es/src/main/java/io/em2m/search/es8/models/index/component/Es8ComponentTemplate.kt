package io.em2m.search.es8.models.index.component

import com.fasterxml.jackson.annotation.JsonProperty
import io.em2m.search.es8.models.Es8IndexSettings
import io.em2m.search.es8.models.index.Es8MappingProperty
import java.util.Date

data class Es8ComponentTemplates(@param:JsonProperty("component_templates") val componentTemplates: List<Es8ComponentTemplateEntry>)

data class Es8ComponentTemplateEntry(val name: String,
                                     @param:JsonProperty("component_template") val componentTemplate: Es8ComponentTemplate)

data class Es8ComponentTemplate(val settings: Es8IndexSettings,
                                val mappings: Es8MappingProperty,
                                val version: Int = 1,
                                @param:JsonProperty("_meta") val meta: Map<String, Any?>? = null,
                                @param:JsonProperty("created_date") val createdDate: Date = Date(),
                                @param:JsonProperty("created_date_millis") val createdDateMillis: Long = createdDate.time,
                                @param:JsonProperty("modified_date") val modifiedDate: Date = Date(),
                                @param:JsonProperty("modified_date_millis") val modifiedDateMillis: Long = modifiedDate.time)
