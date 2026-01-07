package io.em2m.search.es8.models.bulk

// TODO: Map all fields
data class Es8BulkResult(val errors: Boolean, val took: Int, val items: List<Es8BulkItem>)
