package io.em2m.search.migrate.models

import io.em2m.search.es.models.EsVersion
import io.em2m.transactions.TransactionConfig
import io.em2m.transactions.TransactionErrorStrategy

class EsMigrationItem(val config: Map<Class<*>, EsMigrationConfig>) {

    fun classesForStrategy(strategy: EsMigrationStrategy): List<Class<*>> {
        return config.filter { (_, configItem) ->
            configItem.strategy == strategy
        }.map { it.key }
    }

    fun toTransactionConfig(): Map<Class<*>, TransactionConfig> {
        return config.mapValues { (_, esConfig) -> esConfig.toTransactionConfig() }
    }

    operator fun component1(): List<Class<*>> = classesForStrategy(EsMigrationStrategy.PRIMARY)

    operator fun component2(): List<Class<*>> = classesForStrategy(EsMigrationStrategy.PERMISSIVE)

    companion object {

        val DEFAULT = EsMigrationItem(
            config = mutableMapOf(
                EsVersion.DEFAULT.getApi() to EsMigrationConfig.DEFAULT
            )
        )

    }

}

fun interface EsMigrationProvider {

    operator fun get(term: String): EsMigrationItem?

}

enum class EsMigrationStrategy {
    PRIMARY, PERMISSIVE
}

data class EsMigrationConfig(val strategy: EsMigrationStrategy) {

    fun toTransactionConfig(): TransactionConfig {
        val errorStrategy = when (strategy) {
            EsMigrationStrategy.PRIMARY -> TransactionErrorStrategy.ALWAYS
            EsMigrationStrategy.PERMISSIVE -> TransactionErrorStrategy.NEVER
        }
        return TransactionConfig(errorStrategy)
    }

    companion object {
        val DEFAULT = EsMigrationConfig(EsMigrationStrategy.PRIMARY)
    }
}

// per-index mappings of when specific data should be cut over
class EsMigrationMappingItem(var config: Map<EsVersion, EsMigrationConfig> = mutableMapOf()) {

    fun isPrimary(version: EsVersion): Boolean = getStrategy(version) == EsMigrationStrategy.PRIMARY

    fun getStrategy(version: EsVersion): EsMigrationStrategy = run {
        val esConfig = config[version]
        esConfig?.strategy ?: EsMigrationStrategy.PERMISSIVE
    }

    fun versionsForStrategy(strategy: EsMigrationStrategy): List<EsVersion> {
        return config.filter { (_, configItem) ->
            configItem.strategy == strategy
        }.map { it.key }
    }

    operator fun component1(): List<EsVersion> {
        return versionsForStrategy(EsMigrationStrategy.PRIMARY)
    }

    operator fun component2(): List<EsVersion> {
        return versionsForStrategy(EsMigrationStrategy.PERMISSIVE)
    }

}

data class EsMigrationMappingObject(
    val indices: Map<String, EsMigrationMappingItem>,
    val aliases: Map<String, EsMigrationMappingItem> = mutableMapOf()): EsMigrationProvider {

    override operator fun get(term: String): EsMigrationItem? {
        val mappingItem = indices[term] ?: aliases[term] ?: return null
        val classesConfig = mappingItem.config.mapKeys { (version, _) -> version.getApi() }
        return EsMigrationItem(classesConfig)
    }

}
