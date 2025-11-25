package io.em2m.search.migrate.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.obj.MultiCatchingFunctions
import io.em2m.obj.OperationType
import io.em2m.search.es.models.EsVersion
import io.em2m.search.es.EsApi
import io.em2m.utils.FallbackPair

data class EsMigrationItem(val primary: Class<*> = EsApi::class.java,
                           val fallbacks: List<Class<*>> = emptyList(),
                           val undoOnFailure: Boolean = false) {

    fun <T, F> toFallback(primary: T, fallbacks: List<F>): FallbackPair<T, F> {
        if (primary?.javaClass != this.primary.javaClass) throw IllegalArgumentException()
        if (fallbacks.size != this.fallbacks.size) throw IllegalArgumentException()
        if (fallbacks.map { it?.javaClass } != this.fallbacks) throw IllegalArgumentException()
        return FallbackPair(primary, fallbacks)
    }

    fun <T, F> toCatchingFunction(primary: T,
                                  fallbacks: List<F>,
                                  objectMapper: ObjectMapper = jacksonObjectMapper(),
                                  operatorComparator1: ((T, OperationType) -> Int)? = null,
                                  operatorComparator2: ((F, OperationType) -> Int)? = null,
                                  debug: Boolean = false): MultiCatchingFunctions<T, F> {
        if (primary?.javaClass != this.primary.javaClass) throw IllegalArgumentException()
        if (fallbacks.size != this.fallbacks.size) throw IllegalArgumentException()
        if (fallbacks.map { it?.javaClass } != this.fallbacks) throw IllegalArgumentException()
        return MultiCatchingFunctions(
            delegates1 = listOf(primary),
            delegate1Class = primary::class.java,
            delegates2 = fallbacks,
            delegate2Class = fallbacks.firstOrNull()?.javaClass,
            objectMapper= objectMapper,
            operatorComparator1 = operatorComparator1,
            operatorComparator2 = operatorComparator2)
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
                                  val fallbacks: List<EsVersion> = emptyList(),
                                  val undoOnFailure: Boolean = false)

data class EsMigrationMappingObject(
    val indices: Map<String, EsMigrationMappingItem>,
    val aliases: Map<String, EsMigrationMappingItem> = mutableMapOf()): EsMigrationProvider {

    override operator fun get(term: String): EsMigrationItem? {
        val mappingItem = indices[term] ?: aliases[term] ?: return null
        val (primaryVersion, fallbackVersions, undoOnFailure) = mappingItem
        val primaryClass = primaryVersion.getApi()
        val fallbackClasses = fallbackVersions.map(EsVersion::getApi)
        return EsMigrationItem(primaryClass, fallbackClasses, undoOnFailure)
    }

}
