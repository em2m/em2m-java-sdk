package io.em2m.search.es

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.core.model.DocMapper
import io.em2m.search.core.model.IdMapper
import io.em2m.search.es2.dao.Es2SyncDao
import io.em2m.search.es8.Es8Api
import io.em2m.search.es8.dao.Es8SyncDao
import io.em2m.search.migrate.models.EsMigrationItem
import io.em2m.search.transactions.daos.StreamableTransactionDao
import io.em2m.utils.firstIsClass

@Deprecated("Used for migration purposes, replace with the latest EsSyncDao equivalent.")
class EsSyncDao<T : Any> : StreamableTransactionDao<T, EsSyncDaoUnionType<T>> {

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
        val index = this.delegates.first().index
        if (this.delegates.any { delegate -> delegate.index != index }) {
            throw IllegalArgumentException("Currently all index values need to be the same. " +
                "If you want to write to multiple daos, please do so manually.")
        }
    }

    fun withFallbacks(vararg delegates: EsSyncDaoUnionType<T>): EsSyncDao<T> {
        val newDelegates = mutableSetOf<EsSyncDaoUnionType<T>>(this.delegates.first())
        newDelegates.addAll(delegates)
        return EsSyncDao(delegates=newDelegates.toTypedArray())
    }

    class Builder<T: Any> {

        private var delegates: Collection<Any> = mutableListOf()
        fun delegates(delegates: Collection<Any>) = this.apply { this.delegates = delegates }

        private var migrationItem: EsMigrationItem = EsMigrationItem.DEFAULT
        fun migrationItem(migrationItem: EsMigrationItem?) = this.apply { this.migrationItem = migrationItem ?: EsMigrationItem.DEFAULT }

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

        private val required: List<Any?>
            get() = listOf(delegates, index, tClass, idMapper, docMapper, objectMapper)

        fun build(): EsSyncDao<T> {
            if (required.any{it == null}) {
                throw IllegalArgumentException("One or more required fields were null.")
            }
            val delegates = mutableSetOf<EsSyncDaoUnionType<T>>()
            migrationItem.config.mapNotNullTo(delegates) { (clazz, _) ->
                val api = this.delegates.firstIsClass(clazz)
                when (clazz) {
                    EsApi::class.java -> {
                        if (type == null) {
                            throw IllegalArgumentException("Type was null when creating deprecated Es2SyncDao.")
                        }
                        val esApi = api as EsApi
                        val es2SyncDao = Es2SyncDao(esApi, index!!, type!!, tClass!!, idMapper!!, docMapper, objectMapper)
                        es2SyncDao
                    }
                    Es8Api::class.java -> {
                        val es8Api = api as Es8Api
                        val es8SyncDao = Es8SyncDao(es8Api, index!!, tClass!!, idMapper!!, docMapper, objectMapper)
                        es8SyncDao
                    }
                    else -> TODO("Unimplemented API (Future version?)")
                }
            }
            if (delegates.isEmpty()) {
                throw IllegalStateException("Delegates are empty.")
            }
            return EsSyncDao(delegates= delegates.toTypedArray())
        }
    }

}
