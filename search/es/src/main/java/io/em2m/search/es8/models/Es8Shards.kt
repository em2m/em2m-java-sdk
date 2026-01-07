package io.em2m.search.es8.models

data class Es8Shards(val total: Int, val successful: Int, val skipped: Int, val failed: Int)
