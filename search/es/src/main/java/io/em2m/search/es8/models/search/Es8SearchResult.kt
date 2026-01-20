package io.em2m.search.es8.models.search

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.em2m.search.es.EsAggResult
import io.em2m.search.es8.models.Es8Shards

@JsonIgnoreProperties(ignoreUnknown = true)
class Es8SearchResult(val took: Long,
                      @JsonProperty("timed_out") val timedOut: Boolean,
                      @JsonProperty("_shards") val shards: Es8Shards,
                      @JsonProperty("_scroll_id") val scrollId: String? = null,
                      val hits: Es8Hits,
                      val aggregations: Map<String, EsAggResult> = emptyMap()
)
