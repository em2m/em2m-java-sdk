package io.em2m.search.es2.models

import com.fasterxml.jackson.annotation.JsonProperty

@Deprecated("Migrate to Es8Index")
data class Es2Index(
    val health: String,
    val status: String,
    val index: String,
    val pri: Int,
    val rep: Int,
    @param:JsonProperty("docs.count")
    val docsCount: Long,
    @param:JsonProperty("docs.deleted")
    val docsDeleted: Long,
    @param:JsonProperty("store.size")
    val storeSize: String,
    @param:JsonProperty("pri.store.size")
    val priStoreSize: String,
)
