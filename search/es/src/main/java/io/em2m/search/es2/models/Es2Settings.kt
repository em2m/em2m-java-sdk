package io.em2m.search.es2.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es.models.EsVersion
import io.em2m.utils.coerce
import java.util.Date
import java.util.UUID

@Deprecated("Migrate to Es8")
data class Es2MappingSettings(@param:JsonProperty("ignore_malformed") val ignoreMalformed: Boolean = true)

@Deprecated("Migrate to Es8")
data class Es2IndexSettings(
    @param:JsonProperty("creation_date") val creationDate: String = Date().time.toString(),
    val mapping: Es2MappingSettings = Es2MappingSettings(),
    val uuid: String = UUID.randomUUID().toString(),
    @param:JsonProperty("number_of_replicas") val numberOfReplicas: Int = 1,
    @param:JsonProperty("number_of_shards") val numberOfShards: Int = 5,
    @param:JsonProperty("version.created_string") val createdVersion: EsVersion = EsVersion.ES2
)

@Deprecated("Migrate to Es8")
data class Es2Settings(val index: Es2IndexSettings = Es2IndexSettings()) {

    fun toObjectNode(mapper: ObjectMapper = jacksonObjectMapper()): ObjectNode {
        val ret = mapper.createObjectNode()
        ret.put("settings", this.coerce<ObjectNode>(objectMapper = mapper))
        return ret
    }

    companion object

}
