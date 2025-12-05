package io.em2m.search.core.daos

import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.transactions.MultiCatchingFunction
import io.em2m.transactions.OnFailure
import io.em2m.transactions.OperationPrecedence
import io.em2m.transactions.OperationType
import io.em2m.search.core.model.Query
import io.em2m.search.core.model.SearchRequest
import io.em2m.search.core.model.SearchResult
import io.em2m.search.core.model.SyncDao
import io.em2m.utils.Coerce

open class MultiCatchingSyncDao<T, DAO>(vararg delegates: DAO?,
                                        objectMapper: ObjectMapper = Coerce.objectMapper,
                                        val debug: Boolean = false) : MultiCatchingFunction<DAO>(
    delegates=delegates,
    objectMapper=objectMapper,
    operatorComparator = { dao: DAO, type -> dao.getOperationPriority(type) }), SyncDao<T> where DAO : AbstractSyncDao<T> {

    override fun create(entity: T): T? {
        val createOperation = Operation<T, T>(
            OperationType.CREATE,
            OperationPrecedence.ALL,
            { _, _ -> Result.success(true) },
            {
                    delegate, param ->
                delegate.create(param)
            }, OnFailure(undoAction = { delegate, param, _ ->
                val id = delegate.idMapper(param)
                delegate.deleteById(id)
                param
            }))
        return createOperation(entity, debug = debug)
    }

    override fun deleteById(id: String): Boolean {
        val entity = findById(id) ?: return false
        val deleteOperation = Operation<T, Boolean>(
            OperationType.DELETE,
            OperationPrecedence.ALL,
            { _, _ -> Result.success(true) },
            { delegate, entity ->
                val id = delegate.idMapper(entity)
                delegate.deleteById(id)
            }, OnFailure(undoAction = { delegate, entity, initialState ->
                delegate.create(entity)
                true
            }), initialStateFn = { entity })
        return deleteOperation(entity) ?: false
    }

    override fun exists(id: String): Boolean {
        val existsOperation = Operation<String, Boolean>(
            OperationType.READ,
            OperationPrecedence.ANY,
            { _, _ -> Result.success(true) },
            { delegate, param ->
                delegate.exists(param)
            },
        )
        return existsOperation(id,debug = debug) ?: false
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        val searchOperation = Operation<SearchRequest, SearchResult<T>>(
            OperationType.SEARCH,
            OperationPrecedence.ANY,
            { _, _ -> Result.success(true) },
            { delegate, req ->
                delegate.search(req)
            }, combineFn = { list ->
                // these should* already be sorted by priority
                // so searching will prioritize the first dao first, then second, as fallbacks
                val results: List<SearchResult<T>> = list.mapNotNull { (_, result: SearchResult<T>?) ->
                    result
                }
                SearchResult.combineSearchResults(results)
            }
        )
        return searchOperation(request,debug = debug) ?: SearchResult(totalItems = 0)
    }

    override fun count(query: Query): Long {
        // should combine all queries and results into one object and then get the count that way
        // the following implementation **wont** work:

//        val countOperation = Operation<Query, Long>(
//            OperationType.SEARCH,
//            { delegate, param ->
//                delegate.count(param)
//            }, combineFn = { list ->
//                val counts: List<Long> = list.mapNotNull { (_, lng) -> lng }
//                counts.maxOrNull() ?: -1
//            }
//        )
//        return countOperation(query) ?: 0L

        // Should we return the minimum value or somehow combine?
        val countOperation = Operation<Query, Long>(
            OperationType.SEARCH,
            OperationPrecedence.ANY,
            { _, _ -> Result.success(true) },
            { delegate, req ->
                delegate.count(req)
            }
        )
        return countOperation(query,debug = debug) ?: 0L
    }

    override fun findById(id: String): T? {
        val findOperation = Operation<String, T>(
            OperationType.READ,
            OperationPrecedence.ANY,
            { _, _ -> Result.success(true) },
            { delegate, param ->
                delegate.findById(param)
            }
        )
        return findOperation(id,debug = debug)
    }

    override fun findOne(query: Query): T? {
        val findOperation = Operation<Query, T>(
            OperationType.READ,
            OperationPrecedence.ANY,
            { _, _ -> Result.success(true) },
            { delegate, param ->
                delegate.findOne(param)
            }
        )
        return findOperation(query,debug = debug)
    }

    override fun save(id: String, entity: T): T? {
        val saveOperation = Operation<Pair<String, T>, T?>(
            OperationType.UPDATE,
            OperationPrecedence.ALL,
            { _, _ -> Result.success(true) },
            { delegate, pair ->
                val (idParam, entityParam) = pair
                delegate.save(idParam, entityParam)
            },
            onFailure = OnFailure(undoAction = { delegate, pair, initialState ->
                val (idParam, _) = pair
                val (_, initialEntity) = initialState
                delegate.save(idParam, initialEntity)
                initialEntity
            }),
            initialStateFn = {
                findById(id)?.let { entity ->
                    id to entity
                }
            }
        )

        return saveOperation(id to entity,debug = debug)
    }

    private fun generateDelegatesToNewEntities(entities: List<T>): List<Pair<AbstractSyncDao<T>, MutableMap<String, T>>> {
        val delegates = this[OperationType.UPDATE] ?: emptyList()

        val delegatesToEntitiesCache = delegates.map { delegate ->
            delegate to entities.associateBy { delegate.idMapper(it) }.toMutableMap()
        }
        return delegatesToEntitiesCache
    }

    private fun generateDelegatesToEntitiesCache(entities: List<T>): List<Pair<AbstractSyncDao<T>, MutableMap<String, T>>> {
        val delegates = this[OperationType.UPDATE] ?: emptyList()

        // each delegate may map id's differently so you need to see if the dao contains the entity in its own way
        // of storing it before confirming you can roll it back.
        val delegatesToEntitiesCache = delegates.map { delegate ->
            val ids = entities.map { delegate.idMapper(it) }
            val entityState = mutableMapOf<String, T>()
            ids.forEach { id ->
                val previousEntity = delegate.findById(id)
                if (previousEntity != null) {
                    entityState[id] = previousEntity
                }
            }
            delegate to entityState
        }
        return delegatesToEntitiesCache
    }

    private fun rollbackBulkUpdate(delegate: AbstractSyncDao<T>,
                                   newEntities: List<T>,
                                   delegatesToEntitiesCache: List<Pair<AbstractSyncDao<T>, MutableMap<String, T>>>): List<T> {
        // can't assume daos having reliable hash functions, have to resort to good old equals method
        fun List<Pair<AbstractSyncDao<T>, Map<String, T>>>.getEntityCache(template: AbstractSyncDao<T>): Map<String, T>? {
            return this.firstOrNull { (delegate, _) -> delegate == template }?.second
        }
        val initialEntities = delegatesToEntitiesCache.getEntityCache(template = delegate)
        if (initialEntities != null) {
            val created = newEntities.filter { entity ->
                val id = delegate.idMapper(entity)
                id !in initialEntities
            }
            // created
            created.forEach { createdEntity ->
                val id = delegate.idMapper(createdEntity)
                delegate.deleteById(id)
            }
            // updated
            initialEntities.forEach { (id, initialEntity) ->
                delegate.save(id, initialEntity)
            }

        } else {
            logger.error("Initial entities was null! Not removing previous entities as a safe-guard.")
        }
        return initialEntities?.values?.toList() ?: emptyList()
    }

    override fun saveBatch(entities: List<T>): List<T> {

        data class BatchContext(val entities: List<T>, val context: List<Pair<AbstractSyncDao<T>, MutableMap<String, T>>> = emptyList())

        val saveBatchOperation = Operation<BatchContext, List<T>>(
            OperationType.UPDATE,
            OperationPrecedence.ALL,
            { _, _ -> Result.success(true) },
            { delegate, state ->
                delegate.saveBatch(state.entities)
            }, OnFailure(undoAction = { delegate, newState, initialState ->
                val previousEntitiesCache = initialState ?: BatchContext(emptyList())
                rollbackBulkUpdate(delegate, newState.entities, previousEntitiesCache.context)
            }),
            initialStateFn = {
                BatchContext(emptyList(), generateDelegatesToEntitiesCache(entities))
            }
        )
        val input = BatchContext(entities, generateDelegatesToNewEntities(entities))
        return saveBatchOperation(input,debug = debug) ?: emptyList()
    }

    override fun upsert(id: String, entity: T): T? {
        // instead of using pairs it might be better to have specific objects, idk
        val upsertOperation = Operation<Pair<String, T>, T>(
            OperationType.UPDATE,
            OperationPrecedence.ALL,
            { _, _ -> Result.success(true) },
            { delegate, (idParam, tParam) ->
                delegate.upsert(idParam, tParam)
            }
        )
        return upsertOperation(id to entity)
    }

    override fun upsertBatch(entities: List<T>): List<T> {
        data class BatchContext(val entities: List<T>, val context: List<Pair<AbstractSyncDao<T>, MutableMap<String, T>>> = emptyList())

        // this is effectively the same as saving, but upserting is different from saving in databases
        val upsertBatchOperation = Operation<BatchContext, List<T>>(
            OperationType.UPDATE,
            OperationPrecedence.ALL,
            { _, _ -> Result.success(true) },
            { delegate, param ->
                delegate.upsertBatch(param.entities)
            }, OnFailure(undoAction = { delegate, newState, initialState ->
                val previousEntitiesCache = initialState ?: BatchContext(emptyList())
                rollbackBulkUpdate(delegate, newState.entities, previousEntitiesCache.context)
            }),
            initialStateFn = {
                BatchContext(emptyList(), generateDelegatesToEntitiesCache(entities))
            }
        )
        val input = BatchContext(entities, generateDelegatesToNewEntities(entities))
        return upsertBatchOperation(input,debug = debug) ?: emptyList()
    }

    override fun close() {
        val closeOperation = Operation<Unit, Unit>(
            OperationType.IO,
            OperationPrecedence.ALL,
            { _, _ -> Result.success(true) },
            { delegate, _ -> delegate.close() }
        )
        closeOperation(Unit,debug = debug)
        return
    }

}
