package io.em2m.search.es8.models.index

import com.fasterxml.jackson.annotation.JsonProperty
import io.em2m.search.es.EsQuery
import io.em2m.search.es8.models.Es8IndexSettings
import java.util.Date

data class Es8IndexTemplates(@param:JsonProperty("index_templates") val indexTemplates: List<Es8IndexTemplateEntry>)

data class Es8IndexTemplateEntry(val name: String,
                             @param:JsonProperty("index_template") val indexTemplate: Es8IndexTemplate)

data class Es8IndexTemplate(@param:JsonProperty("index_patterns") val indexPatterns: List<String>,
                            val settings: Es8IndexSettings,
                            val mappings: Es8MappingProperty,
                            val aliases: Map<String, Es8AliasSettings> = emptyMap(),
                            @param:JsonProperty("composed_of") val composedOf: List<String>,
                            @param:JsonProperty("ignore_missing_component_templates") val ignoreMissingComponentTemplates: List<String> = emptyList(),
                            val priority: Int = 0,
                            val version: Int = 1,
                            @param:JsonProperty("_meta") val meta: Map<String, Any?>? = null,
                            @param:JsonProperty("created_date") val createdDate: Date = Date(),
                            @param:JsonProperty("created_date_millis") val createdDateMillis: Long = createdDate.time,
                            @param:JsonProperty("modified_date") val modifiedDate: Date = Date(),
                            @param:JsonProperty("modified_date_millis") val modifiedDateMillis: Long = modifiedDate.time)

data class Es8AliasSettings(@param:JsonProperty("is_write_index") val isWriteIndex: Boolean = true,
                            val filter: EsQuery? = null,
                            @param:JsonProperty("is_hidden")
                            val isHidden: Boolean = false,
                            @param:JsonProperty("index_routing") val indexRouting: String? = null,
                            val routing: String? = null,
                            @param:JsonProperty("search_routing") val searchRouting: String? = null)
