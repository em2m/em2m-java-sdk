package io.em2m.search.migrate.models

import io.em2m.search.es.EsApi
import io.em2m.search.es.models.EsVersion
import io.em2m.transactions.TransactionHandler
import io.em2m.utils.FallbackPair

data class EsMigrationItem(val primary: Class<*> = EsApi::class.java,
                           val fallbacks: List<Class<*>> = emptyList()) {

    fun <T, F> toFallback(primary: T, fallbacks: List<F>): FallbackPair<T, F> {
        if (fallbacks.size != this.fallbacks.size) throw IllegalArgumentException()
        return FallbackPair(primary, fallbacks)
    }

    fun <T, F> toTransactionHandler()
    : TransactionHandler {
        if (fallbacks.size != this.fallbacks.size) throw IllegalArgumentException()
        return TransactionHandler()
    }

    companion object {

        val DEFAULT = EsMigrationItem(
            primary = EsVersion.DEFAULT.getApi(),
            fallbacks = emptyList()
        )

    }

}

fun interface EsMigrationProvider {

    operator fun get(term: String): EsMigrationItem?

}

// per-index mappings of when specific data should be cut over
data class EsMigrationMappingItem(val primary: EsVersion = EsVersion.DEFAULT,
                                  val fallbacks: List<EsVersion> = emptyList())

data class EsMigrationMappingObject(
    val indices: Map<String, EsMigrationMappingItem>,
    val aliases: Map<String, EsMigrationMappingItem> = mutableMapOf()): EsMigrationProvider {

    override operator fun get(term: String): EsMigrationItem? {
        val mappingItem = indices[term] ?: aliases[term] ?: return null
        val (primaryVersion, fallbackVersions) = mappingItem
        val primaryClass = primaryVersion.getApi()
        val fallbackClasses = fallbackVersions.map(EsVersion::getApi)
        return EsMigrationItem(primaryClass, fallbackClasses)
    }

}
