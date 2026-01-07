package io.em2m.search.es

import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.search.core.daos.AbstractSyncDao
import io.em2m.search.core.model.DocMapper
import io.em2m.search.core.model.IdMapper
import io.em2m.search.core.model.StreamableDao
import io.em2m.search.es2.dao.Es2SyncDao
import io.em2m.search.es8.Es8Api
import io.em2m.search.es8.dao.Es8SyncDao

// Kotlin doesn't have union types yet*
abstract class EsSyncDaoUnionType<T>(
    val index: String,
    protected val tClass: Class<T>,
    override val idMapper: IdMapper<T>,
    protected open val docMapper: DocMapper<T>? = null,
    protected val objectMapper: ObjectMapper,
    ) : AbstractSyncDao<T>(idMapper), StreamableDao<T> {

    fun toEs2SyncDao(esApi: EsApi, type: String): Es2SyncDao<T> {
        if (this is Es2SyncDao<T>) return this
        return Es2SyncDao(esApi, index, type, tClass, idMapper, docMapper, objectMapper)
    }

    fun toEs8SyncDao(es8Api: Es8Api): Es8SyncDao<T> {
        if (this is Es8SyncDao<T>) return this
        return Es8SyncDao(es8Api, index, tClass, idMapper, docMapper, objectMapper)
    }

}
