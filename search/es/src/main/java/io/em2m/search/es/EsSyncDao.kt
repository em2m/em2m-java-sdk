package io.em2m.search.es

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.core.daos.MultiCatchingStreamableSyncDao
import io.em2m.search.core.model.DocMapper
import io.em2m.search.core.model.IdMapper
import io.em2m.search.es2.dao.Es2SyncDao
import io.em2m.search.es8.Es8Api
import io.em2m.search.es8.dao.Es8SyncDao

// I didn't want to break any existing code mappings for the EsSyncDao
@Deprecated("Used for migration purposes, replace with the latest EsSyncDao equivalent.")
class EsSyncDao<T> : MultiCatchingStreamableSyncDao<T, EsSyncDaoUnionType<T>> {

    @Deprecated("Use EsSyncDao.Builder<T>() or EsMigrationBuilder.EsSyncDao().", ReplaceWith("EsMigrationBuilder().EsSyncDao(index, type, tClass, idMapper, docMapper)", "io.em2m.search.migrate.models.EsMigrationBuilder"))
    constructor(
        esApi: EsApi,
        index: String,
        type: String,
        tClass: Class<T>,
        idMapper: IdMapper<T>,
        docMapper: DocMapper<T>? = null,
        objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule()),
        fallbackDao: EsSyncDaoUnionType<T>? = null
    ): super(Es2SyncDao(esApi, index, type, tClass, idMapper, docMapper, objectMapper),
        fallbackDao)

    @Deprecated("Use EsSyncDao.Builder<T>() instead.")
    constructor(es2Dao: Es2SyncDao<T>, es8Api: Es8Api):
        super(es2Dao, es2Dao.toEs8SyncDao(es8Api))

    private constructor(vararg delegates: EsSyncDaoUnionType<T>):
        super(delegates= delegates)

    init {
        val index = this.primary.index
        if (this.delegates.any { delegate -> delegate.index != index }) {
            throw IllegalArgumentException("Currently all index values need to be the same. " +
                "If you want to write to multiple daos, please do so manually.")
        }
    }

    fun withFallbacks(vararg delegates: EsSyncDaoUnionType<T>): EsSyncDao<T> {
        val newDelegates = mutableSetOf<EsSyncDaoUnionType<T>>(this.primary)
        newDelegates.addAll(delegates)
        return EsSyncDao(delegates=newDelegates.toTypedArray())
    }

    class Builder<T> {

        private var primary: Any? = null

        @Deprecated("For internal use.")
        fun primary(api: Any?): Builder<T> = this.apply { this.primary = api }
        fun primary(esApi: EsApi): Builder<T> = this.primary(esApi as Any)
        fun primary(es8Api: Es8Api): Builder<T> = this.primary(es8Api as Any)

        private var index: String? = null
        fun index(index: String): Builder<T> = this.apply { this.index = index }

        @Deprecated("Removed from Elastic Search.")
        private var type: String? = null
        @Deprecated("Removed from Elastic Search.")
        fun type(type: String?): Builder<T> = this.apply { this.type = type }

        private var tClass: Class<T>? = null
        fun tClass(tClass: Class<T>): Builder<T> = this.apply { this.tClass = tClass }

        private var idMapper: IdMapper<T>? = null
        fun idMapper(idMapper: IdMapper<T>): Builder<T> = this.apply { this.idMapper = idMapper }

        private var docMapper: DocMapper<T>? = null
        fun docMapper(docMapper: DocMapper<T>?): Builder<T> = this.apply { this.docMapper = docMapper }

        private var objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
        fun objectMapper(objectMapper: ObjectMapper): Builder<T> = this.apply { this.objectMapper = objectMapper }

        private var fallbacks: MutableSet<EsSyncDaoUnionType<T>> = mutableSetOf()
        fun fallback(fallback: EsSyncDaoUnionType<T>): Builder<T> = this.apply { this.fallbacks.add(fallback) }
        fun fallbacks(vararg fallbacks: EsSyncDaoUnionType<T>): Builder<T> = this.apply { this.fallbacks.addAll(elements = fallbacks) }

        private val required: List<Any?>
            get() = listOf(primary, index, tClass, idMapper, docMapper, objectMapper)

        fun build(): EsSyncDao<T> {
            if (required.any{it == null}) {
                throw IllegalArgumentException("One or more required fields were null.")
            }
            val delegates = mutableSetOf<EsSyncDaoUnionType<T>>()
            when (this.primary) {
                 is EsApi -> {
                     if (type == null) {
                         throw IllegalArgumentException("Type was null when creating deprecated Es2SyncDao.")
                     }
                     val esApi = this.primary as EsApi
                     val es2SyncDao = Es2SyncDao(esApi, index!!, type!!, tClass!!, idMapper!!, docMapper, objectMapper)
                     delegates.add(es2SyncDao)
                 }
                 is Es8Api -> {
                     val es8Api = this.primary as Es8Api
                     val es8SyncDao = Es8SyncDao(es8Api, index!!, tClass!!, idMapper!!, docMapper, objectMapper)
                     delegates.add(es8SyncDao)
                 }
                 else -> TODO("Unimplemented API (Future version?)")
            }
            delegates.addAll(fallbacks)
            if (delegates.isEmpty()) {
                throw IllegalStateException("Delegates are empty.")
            }
            return EsSyncDao(delegates= delegates.toTypedArray())
        }
    }

}
