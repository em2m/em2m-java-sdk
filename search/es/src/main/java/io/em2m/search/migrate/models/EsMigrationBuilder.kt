package io.em2m.search.migrate.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.core.model.DocMapper
import io.em2m.search.core.model.IdMapper
import io.em2m.search.es.EsSyncDao
import io.em2m.search.es.models.EsVersion
import io.em2m.transactions.AbstractTransactionListener
import io.em2m.transactions.Transaction
import io.em2m.transactions.TransactionContext
import io.em2m.transactions.TransactionHandler
import io.em2m.utils.filterIsClass

// couldn't test this here since we don't have guice
class EsMigrationBuilder(val esMigrationProvider: EsMigrationProvider,
                         val delegates: List<Any>) : AbstractTransactionListener() {

    private val indicesToAliases by lazy {
        val esMultiApi = EsMultiApi(this)
        esMultiApi.getIndicesToAliases()
    }

    fun <INPUT: Any, OUTPUT> toTransactionContext(transaction: Transaction<Any, INPUT, OUTPUT>): TransactionContext<Any, INPUT, OUTPUT> {
        val allowedClasses = EsVersion.VALUES.map(EsVersion::getApi)
        val allowedDelegates = allowedClasses.flatMap { clazz ->
            delegates.filterIsClass(clazz)
        }.toSet().toList()
        return TransactionContext(delegates = allowedDelegates, transaction = transaction)
    }

    fun getTransactionHandler(index: String): TransactionHandler {
        val migrationItem: EsMigrationItem = this.esMigrationProvider[index] ?: EsMigrationItem.DEFAULT
        val config = migrationItem.toTransactionConfig()
        return TransactionHandler(config=config)
    }

    fun <T: Any> EsSyncDao(index: String,
                      type: String? = null,
                      tClass: Class<T>,
                      idMapper: IdMapper<T>,
                      docMapper: DocMapper<T>? = null,
                      objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule()))
    : EsSyncDao<T> {
        var esMigrationItem = esMigrationProvider[index]
        if (esMigrationItem == null) {
            val entry = indicesToAliases.entries.firstOrNull { (_, aliases) ->
                index in aliases
            }
            entry?.let { (indexId, _) ->
                esMigrationItem = esMigrationProvider[indexId]
            }
        }
        esMigrationItem = esMigrationItem ?: EsMigrationItem.DEFAULT

        val allowedClasses = EsVersion.VALUES.map(EsVersion::getApi)
        val allowedDelegates = allowedClasses.flatMap { clazz ->
            delegates.filterIsClass(clazz)
        }.toSet().toList()

        return EsSyncDao.Builder<T>()
            .delegates(allowedDelegates)
            .migrationItem(esMigrationItem)
            .index(index)
            .type(type)
            .tClass(tClass)
            .idMapper(idMapper)
            .docMapper(docMapper)
            .objectMapper(objectMapper)
            .build()
    }


}
