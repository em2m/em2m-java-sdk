package io.em2m.search.es8.models.bulk

data class Es8BulkItem(val index: Es8BulkItemData? = null,
                       val delete: Es8BulkItemData? = null,
                       val create: Es8BulkItemData? = null,
                       val update: Es8BulkItemData? = null)
