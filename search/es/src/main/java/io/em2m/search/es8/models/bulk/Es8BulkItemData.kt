package io.em2m.search.es8.models.bulk

data class Es8BulkItemData(val _index: String,
                           val _id: String,
                           val _version: Int,
                           val result: String? = null,
                           val status: Int)
