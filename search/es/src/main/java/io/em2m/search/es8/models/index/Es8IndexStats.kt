package io.em2m.search.es8.models.index

data class Es8IndexStats(
    val uuid: String,
    val health: String,
    val status: String,
    val primaries: Map<String, Any?> = mutableMapOf(),
    val total: Map<String, Any?> = mutableMapOf())
