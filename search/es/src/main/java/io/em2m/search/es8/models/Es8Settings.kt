package io.em2m.search.es8.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es.models.EsVersion
import io.em2m.search.es2.models.Es2Settings
import io.em2m.utils.coerce
import java.util.Date

data class Es8MappingSettings(@param:JsonProperty("ignore_malformed") val ignoreMalformed: Boolean = true,
                              // TODO: This is just for migration purposes, we need to have a lower total fields limit like 1000
                              @param:JsonProperty("total_fields.limit") val totalFieldsLimit: Int = 1_000_000,
                              @param:JsonProperty("depth.limit") val depthLimit: Int = 20,
                              @param:JsonProperty("nested_fields.limit") val nestedFieldsLimit: Int = 100,
                              @param:JsonProperty("field_name_length.limit") val fieldNameLengthLimit: Int = 1000
)

data class Es8IndexSettings(
    @JsonIgnore
    @param:JsonProperty("creation_date", access = JsonProperty.Access.WRITE_ONLY) val creationDate: String = Date().time.toString(),
    @param:JsonProperty("number_of_shards") val numberOfShards: Int = 5,
    @param:JsonProperty("number_of_replicas") val numberOfReplicas: Int = 1,
    @param:JsonProperty("refresh_interval") val refreshInterval: String = "1s",
    @param:JsonProperty("version.created_string", access = JsonProperty.Access.WRITE_ONLY) val createdString: String = EsVersion.ES8.toString(),
    val mapping: Es8MappingSettings = Es8MappingSettings()
)

data class Es8Settings(val index: Es8IndexSettings = Es8IndexSettings()){

    fun toObjectNode(mapper: ObjectMapper = jacksonObjectMapper()): ObjectNode {
        val ret = mapper.createObjectNode()
        ret.put("settings", this.coerce<ObjectNode>(objectMapper = mapper))
        return ret
    }

    companion object

}
