package io.em2m.search.migrate.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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

    operator fun component2(): List<Class<*>> = classesForStrategy(EsMigrationStrategy.FALLBACK)

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
    PRIMARY, FALLBACK, DEBUG;

    companion object {
        val DEFAULT = FALLBACK
    }
}

data class EsMigrationConfig(val strategy: EsMigrationStrategy, val properties: Map<String, Any?> = mutableMapOf()) {

    fun toTransactionConfig(): TransactionConfig {
        val errorStrategy = when (strategy) {
            EsMigrationStrategy.PRIMARY -> TransactionErrorStrategy.ALWAYS
            EsMigrationStrategy.FALLBACK -> TransactionErrorStrategy.LOG
            EsMigrationStrategy.DEBUG -> TransactionErrorStrategy.NEVER
        }
        return TransactionConfig(errorStrategy, properties)
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
        esConfig?.strategy ?: EsMigrationStrategy.DEFAULT
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
        return versionsForStrategy(EsMigrationStrategy.DEFAULT)
    }

    operator fun component3(): List<EsVersion> {
        return versionsForStrategy(EsMigrationStrategy.DEBUG)
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsMigrationMappingObject(
    val indices: List<Map.Entry<String, EsMigrationMappingItem>>,
    val aliases: List<Map.Entry<String, EsMigrationMappingItem>> = mutableListOf()): EsMigrationProvider {

    private val _indexMap = indices.associate(Map.Entry<String, EsMigrationMappingItem>::toPair)
    private val _aliasesMap = aliases.associate(Map.Entry<String, EsMigrationMappingItem>::toPair)

    override operator fun get(term: String): EsMigrationItem? {
        val mappingItem = _indexMap[term] ?: _aliasesMap[term] ?: return null
        val classesConfig = mappingItem.config.mapKeys { (version, _) -> version.getApi() }
        return EsMigrationItem(classesConfig)
    }

    companion object {
        val DEFAULT = EsMigrationMappingObject(mutableListOf(), mutableListOf())
    }

}
