package io.em2m.search.es2.models

@Deprecated("Replace with Es8Mapping", ReplaceWith("io.em2m.search.es8.models.index.Es8Mapping"))
data class Es2Mapping(val index: String, val type: String, val properties: Map<String, Any?>)
