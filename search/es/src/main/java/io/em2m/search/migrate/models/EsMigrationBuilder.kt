package io.em2m.search.migrate.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.transactions.MultiFunctions
import io.em2m.search.core.model.DocMapper
import io.em2m.search.core.model.IdMapper
import io.em2m.search.es.EsApi
import io.em2m.search.es.EsSyncDao
import io.em2m.search.es8.Es8Api
import io.em2m.utils.FallbackPair
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Proxy
import kotlin.collections.component1

// have esapi and es8api be parameters which get injected
// couldn't test this here since we don't have guice
class EsMigrationBuilder(val esMigrationMapping: EsMigrationProvider,
                         primary: EsApi,
                         val fallback: Es8Api? = null) : FallbackPair<EsApi, Es8Api>(primary, listOfNotNull(fallback)) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val indicesToAliases: Map<String, List<String>> by lazy {
        getOrThrow(EsApi::getIndicesToAliases, Es8Api::getIndicesToAliases)
    }

    operator fun get(index: String): MultiFunctions<EsApi, Es8Api> {
        val migrationItem: EsMigrationItem = this.esMigrationMapping[index] ?: EsMigrationItem.DEFAULT
        return migrationItem.toCatchingFunction(primary, fallbacks = fallbacks)
    }

    fun getAny(): MultiFunctions<EsApi, Es8Api> {
        val migrationItem = EsMigrationItem(primary= this.primary.javaClass, fallbacks = this.fallbacks.map { it.javaClass })
        return migrationItem.toCatchingFunction(primary, fallbacks= fallbacks)
    }

    fun <T> EsSyncDao(index: String,
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
            .map(::buildFrom)
            .map(EsSyncDao<T>::primary).toTypedArray()
        return primarySyncDao.withFallbacks(delegates = fallbackDaos)
    }


}
