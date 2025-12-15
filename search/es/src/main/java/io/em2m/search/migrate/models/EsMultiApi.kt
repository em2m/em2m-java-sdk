package io.em2m.search.migrate.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.transactions.OnFailure
import io.em2m.transactions.TransactionPrecedence
import io.em2m.transactions.TransactionType
import io.em2m.search.es.EsAliasAction
import io.em2m.search.es.EsAliasRequest
import io.em2m.search.es2.models.Es2Settings
import io.em2m.search.es8.models.Es8Settings
import io.em2m.search.es8.models.auth.CREATE_INDEX_PRIVILEGE
import io.em2m.search.es8.models.auth.DELETE_INDEX_PRIVILEGE
import io.em2m.search.es8.models.auth.MANAGE_INDEX_PRIVILEGE
import io.em2m.search.migrate.toLegacy
import io.em2m.search.migrate.toModern

class EsMultiApi(private val esMigrationBuilder: EsMigrationBuilder) {

    fun createIndex(index: String): Boolean {
        val function = esMigrationBuilder[index]
        val createIndexOperation = function.Operation<String, Boolean>(
            TransactionType.CREATE,
            TransactionPrecedence.ALL,
            condition = { es1, es8, input ->
                val indexExists = es1.exists(input) || es8.exists(input)
                val allowed = es8.hasIndexPrivilege(input, CREATE_INDEX_PRIVILEGE)

                if (!allowed) {
                    return@Operation Result.failure(IllegalStateException("Can't create index: insufficient privilege $CREATE_INDEX_PRIVILEGE"))
                }
                if (indexExists) {
                    return@Operation Result.failure(IllegalStateException("Can't create index: index exists"))
                }
                Result.success(true)
            },
            tryFn1 = { es1, input ->
                es1.createIndex(input)
                true
            },
            tryFn2 = { es8, input ->
                es8.createIndex(input)
                true
            },
            onFailure1 = OnFailure(undoAction = { es1, input, _ ->
                es1.deleteIndex(input)
                false
            }),
            onFailure2 = OnFailure(undoAction = { es8, input, _ ->
                es8.deleteIndex(input)
                false
            })
        )
        return createIndexOperation(index) ?: false
    }

    fun createIndex(index: String, settings: EsSettings, mapper: ObjectMapper = jacksonObjectMapper()): Boolean {
        data class CreateIndexScope(val index: String, val settings: EsSettings)
        val scope = CreateIndexScope(index, settings)

        val function = esMigrationBuilder[index]
        val createIndexOperation = function.Operation<CreateIndexScope, Boolean>(
            TransactionType.CREATE,
            TransactionPrecedence.ALL,
            condition = { es1, es8, (index) ->
                val allowed = es8.hasIndexPrivilege(index, CREATE_INDEX_PRIVILEGE)

                if (!allowed) {
                    return@Operation Result.failure(IllegalStateException("Can't create index: insufficient privilege $CREATE_INDEX_PRIVILEGE"))
                }

                val indexExists = es1.exists(index) || es8.exists(index)
                if (indexExists) {
                    return@Operation Result.failure(IllegalStateException("Can't create index: index exists"))
                }
                Result.success(true)
            },
            tryFn1 = { es1, (index, settings) ->
                es1.createIndex(index, settings.old.toObjectNode(mapper))
                true
            },
            tryFn2 = { es8, (index, settings) ->
                es8.createIndex(index, settings.new.toObjectNode(mapper))
                true
            },
            onFailure1 = OnFailure(undoAction = { es1, (index), _ ->
                es1.deleteIndex(index)
                false
            }),
            onFailure2 = OnFailure(undoAction = { es8, (index), _ ->
                es8.deleteIndex(index)
                false
            })
        )
        return createIndexOperation(scope) ?: false
    }

    fun createIndex(index: String, settings: ObjectNode, mapper: ObjectMapper = jacksonObjectMapper()): Boolean {
        return this.createIndex(index, EsSettings.fromObjectNode(settings, mapper), mapper)
    }

    fun createIndex(index: String, settings: Es8Settings, mapper: ObjectMapper = jacksonObjectMapper()): Boolean {
        return this.createIndex(index, EsSettings(settings.toLegacy(), settings), mapper)
    }

    @Deprecated("Migrate to Es8Settings")
    fun createIndex(index: String, settings: Es2Settings, mapper: ObjectMapper = jacksonObjectMapper()): Boolean {
        return this.createIndex(index, EsSettings(settings, settings.toModern()), mapper)
    }

    @Deprecated("Deleting an index is dangerous, please only do this when the data exists somewhere else.")
    fun deleteIndex(index: String): Boolean {
        val function = esMigrationBuilder[index]
        val deleteIndexOperation = function.Operation<String, Boolean>(
            TransactionType.DELETE,
            TransactionPrecedence.ALL,
            condition = { es1, es8, input ->
                val allowed = es8.hasIndexPrivilege(input, DELETE_INDEX_PRIVILEGE)
                if (!allowed) {
                    return@Operation Result.failure(IllegalStateException("Can't delete index: insufficient privilege $DELETE_INDEX_PRIVILEGE"))
                }

                val indexExists = es1.exists(input) || es8.exists(input)
                if (!indexExists) {
                    return@Operation Result.failure(IllegalStateException("Can't delete index."))
                }

                Result.success(true)
            },
            tryFn1 = { es1, input ->
                es1.deleteIndex(input)
                true
            },
            tryFn2 = { es8, input ->
                es8.deleteIndex(input)
                true
            },
            onFailure1 = OnFailure(undoAction = { es1, input, _ ->
                // TODO: figure out how to un-delete?
                false
            }),
            onFailure2 = OnFailure(undoAction = { es8, input, _ ->
                // TODO: figure out how to un-delete?
                false
            }
        ))
        return deleteIndexOperation(index) ?: false
    }

    fun getMetadata(): ObjectNode? {
        val function = esMigrationBuilder.getAny()
        val operation = function.Operation<Unit, ObjectNode>(
            TransactionType.READ,
            TransactionPrecedence.ANY,
            tryFn1 = { es1, _ ->
                es1.getMetadata()
            },
            tryFn2 = { es8, _ ->
                es8.getMetadata()
            }
        )
        return operation(Unit)
    }

    fun putAliases(request: EsAliasRequest): Boolean {
        val indices: Set<String> = request.actions.flatMap { listOfNotNull(it.add?.index, it.remove?.index ) }.toSet()
        val function = esMigrationBuilder.getAny()
        val operation = function.Operation<EsAliasRequest, Boolean>(
            TransactionType.IO,
            TransactionPrecedence.ALL,
            condition = {es1, es8, _ ->
                val indicesExist = indices.all {  es1.exists(it) && es8.exists(it) }
                if (!indicesExist) {
                    return@Operation Result.failure(IllegalStateException("Can't put aliases: indices do not exist for all delegates."))
                }

                val allowed = indices.all { index -> es8.hasIndexPrivilege(index, MANAGE_INDEX_PRIVILEGE) }

                if (!allowed) {
                    return@Operation Result.failure(IllegalStateException("Can't put aliases: insufficient privilege $MANAGE_INDEX_PRIVILEGE"))
                }

                Result.success(true)
            },
            tryFn1 = { es1, aliasRequest ->
                es1.putAliases(aliasRequest)
                true
            },
            onFailure1 = OnFailure(undoAction = { es1, _, initialState ->
                initialState?.let {
                    es1.putAliases(initialState)
                }
                false
            }),
            tryFn2 = { es8, aliasRequest ->
                es8.putAliases(aliasRequest)
                true
            },
            onFailure2 = OnFailure(undoAction = { es8, _, initialState ->
                initialState?.let {
                    es8.putAliases(initialState)
                }
                false
            }),
            initialStateFn = {
                // invert actions
                val newActions = request.actions.map { action ->
                    EsAliasAction(add= action.remove, remove= action.add)
                }.toMutableList()
                EsAliasRequest().apply { actions = newActions }
            }
        )
        return operation(request) ?: false
    }

    fun getIndicesToAliases(): Map<String, List<String>> {
        val function = esMigrationBuilder.getAny()
        val operation = function.Operation<Unit, Map<String, List<String>>>(
            TransactionType.READ,
            TransactionPrecedence.ALL,
            tryFn1 = { es1, _ ->
                es1.getIndicesToAliases()
            },
            tryFn2 = { es8, _ ->
                es8.getIndicesToAliases()
            },
            combineFn = { delegatesToMaps ->
                val retSetMap = mutableMapOf<String, MutableSet<String>>()
                delegatesToMaps.forEach { (_, map) ->
                    map?.forEach { (key, value) ->
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

        )
        return operation(Unit) ?: emptyMap()
    }

    fun getAliases(index: String): List<String> {
        val indicesToAliases = this.getIndicesToAliases()
        return indicesToAliases[index] ?: emptyList()
    }

    fun exists(index: String): Boolean {
        val function = esMigrationBuilder.getAny()
        val operation = function.Operation<String, Boolean>(
            TransactionType.IO,
            TransactionPrecedence.ALL,
            tryFn1 = { es1, param ->
                es1.exists(param)
            },
            tryFn2 = { es8, param ->
                es8.exists(param)
            },
            combineFn = { delegatesToBooleans ->
                delegatesToBooleans.all { (_, value) ->
                    value ?: false
                }
            }
        )
        return operation(index) ?: false
    }


}
