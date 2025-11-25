package io.em2m.search.es2.models

import io.em2m.search.es.models.EsStatMapping

@Deprecated("Replace with Es8Stats")
data class Es2Stats(val _shards: Map<String, Any?>, val _all: EsStatMapping, val indices: Map<String, EsStatMapping>) {

    operator fun get(index: String): EsStatMapping? = indices[index]

}
