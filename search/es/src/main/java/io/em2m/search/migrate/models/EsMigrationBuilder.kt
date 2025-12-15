package io.em2m.search.migrate.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.core.model.DocMapper
import io.em2m.search.core.model.IdMapper
import io.em2m.search.es.EsApi
import io.em2m.search.es.EsSyncDao
import io.em2m.search.es.EsSyncDaoUnionType
import io.em2m.search.es8.Es8Api
import io.em2m.transactions.TransactionContext
import io.em2m.transactions.TransactionHandler
import io.em2m.utils.FallbackPair
import java.lang.reflect.Proxy

// couldn't test this here since we don't have guice
class EsMigrationBuilder(val esMigrationMapping: EsMigrationProvider,
                         primary: EsApi,
                         val fallback: Es8Api? = null) : FallbackPair<EsApi, Es8Api>(primary, listOfNotNull(fallback)) {

    private val indicesToAliases: Map<String, List<String>> by lazy {
        getOrThrow(EsApi::getIndicesToAliases, Es8Api::getIndicesToAliases)
    }

    fun <INPUT: Any, OUTPUT> toTransactionContext(): TransactionContext<Any, INPUT, OUTPUT> {
        return TransactionContext(delegates = listOfNotNull(this.primary, fallback))
    }

    private fun getDelegateFromClass(clazz: Class<*>): Any? {
        val allDelegates: List<Any> = mutableListOf<Any>(this.primary).apply { fallback?.let { add(fallback) } }
        val api: Any? = allDelegates.firstOrNull { delegate ->
            if (delegate is Proxy) {
                val proxy = allDelegates.first() as? Proxy
                val interfaces = proxy?.javaClass?.interfaces ?: arrayOf()
                if (clazz in interfaces) {
                    return@firstOrNull true
                }
            }
            delegate.javaClass == clazz
        }
        return api
    }

    fun <INPUT: Any, OUTPUT> toTransactionContext(index: String): TransactionContext<Any, INPUT, OUTPUT> {
        val migrationItem: EsMigrationItem = this.esMigrationMapping[index] ?: EsMigrationItem.DEFAULT
        val classes = mutableListOf(migrationItem.primary).union(migrationItem.fallbacks)
        val delegates = classes.mapNotNull(::getDelegateFromClass)
        return TransactionContext(delegates = delegates)
    }

    fun getTransactionHandler(): TransactionHandler {
        return TransactionHandler()
    }

    fun <T: Any> EsSyncDao(index: String,
                      type: String? = null,
                      tClass: Class<T>,
                      idMapper: IdMapper<T>,
                      docMapper: DocMapper<T>? = null,
                      objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule()))
    : EsSyncDao<T> {
        var esMigrationItem = esMigrationMapping[index]
        if (esMigrationItem == null) {
            val entry = indicesToAliases.entries.firstOrNull { (_, aliases) ->
                index in aliases
            }
            entry?.let { (indexId, _) ->
                esMigrationItem = esMigrationMapping[indexId]
            }
        }
        val (primary, fallbacks) = esMigrationItem ?: EsMigrationItem.DEFAULT

        fun buildFrom(clazz: Class<*>): EsSyncDao<T> {
            val api = getDelegateFromClass(clazz)
            return EsSyncDao.Builder<T>()
                .primary(api)
                .index(index)
                .type(type)
                .tClass(tClass)
                .idMapper(idMapper)
                .docMapper(docMapper)
                .objectMapper(objectMapper)
                .build()
        }

        val primarySyncDao = buildFrom(primary)
        val fallbackDaos = fallbacks
            .mapNotNull(::getDelegateFromClass)
            .mapNotNull { it as? EsSyncDaoUnionType<T> }
            .toTypedArray()
        return primarySyncDao.withFallbacks(delegates = fallbackDaos)
    }


}
