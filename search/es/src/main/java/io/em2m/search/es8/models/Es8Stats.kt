package io.em2m.search.es8.models

import io.em2m.search.es.models.EsStatMapping
import io.em2m.search.es8.models.index.Es8IndexStats

class Es8Stats(val _shards: Map<String, Any?>, val _all: EsStatMapping, val indices: Map<String, Es8IndexStats>) {

    operator fun get(index: String): Es8IndexStats? = indices[index]

}
