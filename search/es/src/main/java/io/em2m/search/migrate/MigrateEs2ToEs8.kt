package io.em2m.search.migrate

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.ES_ACCEPTED_TYPES
import io.em2m.search.es.isEsLeafType
import io.em2m.search.es.isEsTypeNumeric
import io.em2m.search.es2.models.Es2GeoPointProperty
import io.em2m.search.es2.models.Es2Mapping
import io.em2m.search.es2.models.Es2MappingProperty
import io.em2m.search.es8.models.Es8Dynamic
import io.em2m.search.es8.models.index.Es8Mapping
import io.em2m.search.es8.models.index.Es8MappingProperty
import io.em2m.search.es8.models.index.properties.Es8GeoPointProperty
import io.em2m.utils.coerce

fun migrateEs2ToEs8(es2Property: Es2MappingProperty): Es8MappingProperty {
    return migrateEs2ToEs8(es2Property, 0)
}

private fun migrateEs2ToEs8(es2Property: Es2MappingProperty, _depth: Int = 0): Es8MappingProperty {
    val type: String? = when (es2Property.type?.trim()) {
        "geo_point" -> {
            return if (es2Property is Es2GeoPointProperty) {
                // TODO: possibly include `null_value`: `0,0` for geo points
                Es8GeoPointProperty()
            } else {
                Es8GeoPointProperty()
            }
        }
        "string" -> {
            if (es2Property.index == "not_analyzed") {
                "keyword"
            } else {
                "text"
            }
        }
        "object", null, "" -> {
            if (_depth == 0 || es2Property.properties.isNullOrEmpty()) {
                null
            } else {
                "object"
            }
        }
        else -> {
            if (es2Property.type in ES_ACCEPTED_TYPES && _depth > 1) {
                es2Property.type
            } else if (isEsLeafType(es2Property.type)){
                es2Property.type
            } else {
                null
            }
        }
    }
    val format: String? = if (type == "date") {
        es2Property.format
    } else if (type != null && es2Property.format != null){
        // TODO: Decide how to auto-map formats
        throw UnsupportedOperationException("Figure this out")
    } else {
        null
    }

    // Make sure objects don't blow up the index
    val dynamic: Boolean? = if (type == "object") {
        es2Property.dynamic ?: false
    } else {
        null
    }

    if (type == null && _depth > 0 && es2Property.properties.isNullOrEmpty()) {
        // there are some weird edge cases where sub properties don't map correctly
        return Es8MappingProperty(
            type= "object",
            properties = mutableMapOf(),
            dynamic = Es8Dynamic.FALSE
        )
    }

    return Es8MappingProperty(
        type=type,
        properties = es2Property.properties?.mapValues{ (_, value) ->
            migrateEs2ToEs8(value, _depth + 1)}
                ?.filterKeys(String::isNotBlank)
                ?.toMutableMap()
            ,
        format = format,
        dynamic = Es8Dynamic.fromBoolean(dynamic)
    )
}

fun migrateEs2ToEs8(es2Mapping: Es2Mapping, objectMapper: ObjectMapper = jacksonObjectMapper()): Es8Mapping {
    val es2Property = es2Mapping.coerce<Es2MappingProperty>(objectMapper= objectMapper)
    requireNotNull(es2Property) { "es2Property could not be mapped" }
    return Es8Mapping(index=es2Mapping.index, properties=migrateEs2ToEs8(es2Property))
}
