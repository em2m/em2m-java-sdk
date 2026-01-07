package io.em2m.search.es8.models.search

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Es8Hits(@param:JsonProperty("total") private val _total: Es8Total,
                   @param:JsonProperty("max_score") val maxScore: Any? = null,
                   val hits: List<Es8Hit>) {

    val total: Long
        get() = this._total.value

}
