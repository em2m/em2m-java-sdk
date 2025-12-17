package io.em2m.search.migrate.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es.EsAliasAction
import io.em2m.search.es.EsAliasRequest
import io.em2m.search.es.EsApi
import io.em2m.search.es2.models.Es2Settings
import io.em2m.search.es8.Es8Api
import io.em2m.search.es8.models.Es8Settings
import io.em2m.search.es8.models.auth.*
import io.em2m.search.migrate.toLegacy
import io.em2m.search.migrate.toModern
import io.em2m.transactions.AbstractTransactionListener
import io.em2m.transactions.Transaction
import io.em2m.transactions.TransactionPrecedence
import io.em2m.transactions.TransactionType

open class EsMultiApi(private val esMigrationBuilder: EsMigrationBuilder,
                      private val mapper: ObjectMapper = jacksonObjectMapper()): AbstractTransactionListener() {

    protected open val createIndexTransaction: Transaction<Any, String, Boolean> by lazy {
        val transaction = Transaction.Builder<Any, String, Boolean>()
            .condition { delegate, context ->
                val index = context.input!!
                return@condition when (delegate) {
                    is EsApi -> { !delegate.exists(index) }
                    is Es8Api -> { !delegate.exists(index) && delegate.hasIndexPrivilege(index, CREATE_INDEX_PRIVILEGE) }
                    else -> true
                }
            }
            .main { delegate, context ->
                when (delegate) {
                    is EsApi -> { delegate.createIndex(context.input!!) }
                    is Es8Api -> { delegate.createIndex(context.input!!) }
                    else -> {
                        TODO("Unsupported class")
                        false
                    }
                }
                true
            }
            .type(TransactionType.CREATE)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    fun createIndex(index: String): Boolean {
        val handler = esMigrationBuilder.getTransactionHandler()
        val context = esMigrationBuilder.toTransactionContext(index, createIndexTransaction)

        val input = index
        return handler(context, input).getOrThrow() ?: false
    }

    protected val createIndexWithSettingsTransaction by lazy {
        val transaction = Transaction.Builder<Any, Pair<String, EsSettings>, Boolean>()
            .condition { delegate, context ->
                val (index, _) = context.input!!
                return@condition when (delegate) {
                    is EsApi -> { !delegate.exists(index) }
                    is Es8Api -> { !delegate.exists(index) && delegate.hasIndexPrivilege(index, CREATE_INDEX_PRIVILEGE) }
                    else -> true
                }
            }
            .main { delegate, context ->
                val (index, settings) = context.input!!
                when (delegate) {
                    is EsApi -> { delegate.createIndex(index, settings.old.toObjectNode(mapper) )}
                    is Es8Api -> { delegate.createIndex(index, settings.new.toObjectNode(mapper) )}
                    else -> {}
                }
                true
            }
            .type(TransactionType.CREATE)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    fun createIndex(index: String, settings: EsSettings): Boolean {
        val handler = esMigrationBuilder.getTransactionHandler()
        val context = esMigrationBuilder.toTransactionContext(index, createIndexWithSettingsTransaction)

        val input = index to settings
        return handler(context, input).getOrThrow() ?: false
    }

    fun createIndex(index: String, settings: ObjectNode): Boolean {
        return this.createIndex(index, EsSettings.fromObjectNode(settings, mapper))
    }

    fun createIndex(index: String, settings: Es8Settings): Boolean {
        return this.createIndex(index, EsSettings(settings.toLegacy(), settings))
    }

    @Deprecated("Migrate to Es8Settings")
    fun createIndex(index: String, settings: Es2Settings): Boolean {
        return this.createIndex(index, EsSettings(settings, settings.toModern()))
    }

    protected open val deleteIndexTransaction: Transaction<Any, String, Boolean> by lazy {
        val transaction = Transaction.Builder<Any, String, Boolean>()
            .condition { delegate, context ->
                val index = context.input!!
                return@condition when (delegate) {
                    is EsApi -> { delegate.exists(index) }
                    is Es8Api -> { delegate.exists(index) && delegate.hasIndexPrivilege(index, DELETE_INDEX_PRIVILEGE) }
                    else -> true
                }
            }
            .main { delegate, context ->
                when (delegate) {
                    is EsApi -> { delegate.createIndex(context.input!!) }
                    is Es8Api -> { delegate.createIndex(context.input!!) }
                    else -> {
                        TODO("Unsupported class")
                        false
                    }
                }
                true
            }
            .type(TransactionType.DELETE)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    @Deprecated("Deleting an index is dangerous, please only do this when the data exists somewhere else.")
    fun deleteIndex(index: String): Boolean {
        val handler = esMigrationBuilder.getTransactionHandler()
        val context = esMigrationBuilder.toTransactionContext(index, deleteIndexTransaction)

        val input = index
        return handler(context, input).getOrThrow() ?: false
    }

    protected open val getMetadataTransaction: Transaction<Any, Unit, ObjectNode?> by lazy {
        val transaction = Transaction.Builder<Any, Unit, ObjectNode?>()
            .main { delegate, _ ->
                when(delegate) {
                    is EsApi -> delegate.getMetadata()
                    is Es8Api -> delegate.getMetadata()
                    else -> null
                }
            }
            .type(TransactionType.READ)
            .precedence(TransactionPrecedence.ANY)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    fun getMetadata(): ObjectNode? {
        val handler = esMigrationBuilder.getTransactionHandler()
        val context = esMigrationBuilder.toTransactionContext(getMetadataTransaction)

        val input = Unit
        return handler(context, input).getOrNull()
    }

    protected open val putAliasesTransaction by lazy {
        val transaction = Transaction.Builder<Any, EsAliasRequest, Boolean>()
            .condition { delegate, context ->
                val request = context.input!!
                val indices: Set<String> = request.actions.flatMap { listOfNotNull(it.add?.index, it.remove?.index ) }.toSet()

                when (delegate) {
                    is EsApi -> {
                        indices.all { index -> delegate.exists(index) }
                    }
                    is Es8Api -> {
                        indices.all { index -> delegate.hasIndexPrivilege(index, MANAGE_INDEX_PRIVILEGE) } &&
                            indices.all { index -> delegate.exists(index) }
                    }
                    else -> true
                }
            }
            .initialValue { context ->
                val request = context.input!!
                val newActions = request.actions.map { action ->
                    EsAliasAction(add= action.remove, remove= action.add)
                }.toMutableList()
                EsAliasRequest().apply { actions = newActions }
            }
            .main { delegate, context ->
                val request = context.input!!
                when (delegate) {
                    is EsApi -> { delegate.putAliases(request) }
                    is Es8Api -> { delegate.putAliases(request) }
                    else -> {
                        //pass
                    }
                }
                true
            }
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    fun putAliases(request: EsAliasRequest): Boolean {
        val handler = esMigrationBuilder.getTransactionHandler()
        val context = esMigrationBuilder.toTransactionContext(putAliasesTransaction)

        val input = request
        return handler(context, input).getOrNull() ?: false
    }

    protected val getIndicesToAliasesTransaction: Transaction<Any, Unit, Map<String, List<String>>> by lazy {
        val transaction= Transaction.Builder<Any, Unit, Map<String, List<String>>>()
            .main { delegate, _ ->
                when (delegate) {
                    is EsApi -> { delegate.getIndicesToAliases() }
                    is Es8Api -> { delegate.getIndicesToAliases() }
                    else -> {
                        mutableMapOf()
                    }
                }
            }
            .combine { maps ->
                val retSetMap = mutableMapOf<String, MutableSet<String>>()
                maps.forEach { map ->
                    map.forEach { (key, value) ->
                        val set = retSetMap.computeIfAbsent(key) {
                            value.toMutableSet()
                        }
                        set.addAll(value)
                        retSetMap[key] = set
                    }
                }
                val retMap = mutableMapOf<String, List<String>>()
                retSetMap.forEach { (key, values) ->
                    retMap[key] = values.toList()
                }
                retMap
            }
            .type(TransactionType.READ)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    fun getIndicesToAliases(): Map<String, List<String>> {
        val handler = esMigrationBuilder.getTransactionHandler()
        val context = esMigrationBuilder.toTransactionContext(getIndicesToAliasesTransaction)

        val input = Unit
        return handler(context, input).getOrThrow() ?: emptyMap()
    }

    fun getAliases(index: String): List<String> {
        val indicesToAliases = this.getIndicesToAliases()
        return indicesToAliases[index] ?: emptyList()
    }

    protected val existsTransaction: Transaction<Any, String, Boolean> by lazy {
        val transaction = Transaction.Builder<Any, String, Boolean>()
            .main { delegate, context ->
                val index = context.input!!
                when (delegate) {
                    is EsApi -> { delegate.exists(index) }
                    is Es8Api -> { delegate.exists(index) }
                    else -> { false }
                }
            }
            .combine { values -> values.any{it} }
            .type(TransactionType.READ)
            .precedence(TransactionPrecedence.ANY)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    fun exists(index: String): Boolean {
        val handler = esMigrationBuilder.getTransactionHandler()
        val context = esMigrationBuilder.toTransactionContext(existsTransaction)

        val input = index
        return handler(context, input).getOrThrow() ?: false
    }


}
