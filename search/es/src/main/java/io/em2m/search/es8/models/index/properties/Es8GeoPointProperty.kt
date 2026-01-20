package io.em2m.search.es8.models.index.properties

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.em2m.search.es8.models.index.Es8MappingProperty

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class Es8GeoPointProperty(@param:JsonProperty("null_value") val nullValue: String? = null) : Es8MappingProperty(type= "geo_point")
