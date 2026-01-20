package io.em2m.search.es8.models.search

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class Es8Hit(@param:JsonProperty("_index")     val index: String,
                  @param:JsonProperty("_id")        val id: String,
                  @param:JsonProperty("_ignored")   val ignored: List<String> = listOf(),
                  @param:JsonProperty("_score")     val score: Double,
                  @param:JsonProperty("_source")    val source: ObjectNode? = null) {

    companion object

}
