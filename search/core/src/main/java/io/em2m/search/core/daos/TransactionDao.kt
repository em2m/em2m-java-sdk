package io.em2m.search.core.daos

import io.em2m.search.core.model.Query
import io.em2m.search.core.model.SearchRequest
import io.em2m.search.core.model.SearchResult
import io.em2m.search.core.model.SyncDao
import io.em2m.transactions.*

open class TransactionDao<T : Any, DAO>(val delegates: List<DAO>, val config: Map<Class<*>, TransactionConfig> = mutableMapOf())
    : AbstractTransactionListener(), SyncDao<T> where DAO : SyncDao<T> {

    protected val handler = object : TransactionHandler(config) {
        override fun getTransactionPriority(delegate: Any?, context: TransactionContext<*, *, *>): Int {
            if (delegate is SyncDao<*>) {
                return delegate.getTransactionPriority(context.transaction.type)
            }
            return super.getTransactionPriority(delegate, context)
        }
    }

    protected open val createTransaction: Transaction<SyncDao<T>, T, T?> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, T, T?>()
            .main { delegate, context -> delegate.create(context.input!!) }
            .type(TransactionType.CREATE)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }
    override fun create(entity: T): T? {
        val context = createTransaction.toContext(delegates)
        val input = entity
        return handler(context, input).getOrNull()
    }

    protected val deleteByIdTransaction : Transaction<SyncDao<T>, Pair<String, T?>, Boolean> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, Pair<String, T?>, Boolean>()
            .initialValue { context ->
                val (id, _) = context.input!!
                id to findById(id)
            }
            .main { delegate, context -> delegate.deleteById(context.input!!.first) }
            .type(TransactionType.DELETE)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun deleteById(id: String): Boolean {
        val context = deleteByIdTransaction.toContext(delegates)
        val input = id to null
        return handler(context, input).getOrNull() ?: false
    }

    protected open val existsTransaction: Transaction<SyncDao<T>, String, Boolean> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, String, Boolean>()
            .main { delegate, context -> delegate.exists(context.input!!) }
            .type(TransactionType.READ)
            .precedence(TransactionPrecedence.ANY)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun exists(id: String): Boolean {
        val context = existsTransaction.toContext(delegates)
        val input = id
        return handler(context, input).getOrNull() ?: false
    }

    protected open val searchTransaction: Transaction<SyncDao<T>, SearchRequest, SearchResult<T>> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, SearchRequest, SearchResult<T>>()
            .main { delegate, context ->
                delegate.search(context.input!!)
            }
            .type(TransactionType.SEARCH)
            .precedence(TransactionPrecedence.ANY)
            .combine { SearchResult.combineSearchResults(it) }
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        val context = searchTransaction.toContext(delegates)
        val input = request
        return handler(context, input).getOrThrow()!!
    }

    protected open val countTransaction: Transaction<SyncDao<T>, Query, Long> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, Query, Long>()
            .main { delegate, context -> delegate.count(context.input!!) }
            .type(TransactionType.SEARCH)
            .precedence(TransactionPrecedence.ANY)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun count(query: Query): Long {
        val context = countTransaction.toContext(delegates)
        val input = query
        return handler(context, input).getOrNull() ?: 1L
    }

    protected open val findByIdTransaction: Transaction<SyncDao<T>, String, T?> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, String, T?>()
            .main { delegate, context -> delegate.findById(context.input!!) }
            .type(TransactionType.READ)
            .precedence(TransactionPrecedence.ANY)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun findById(id: String): T? {
        val context = findByIdTransaction.toContext(delegates)
        val input = id
        return handler(context, input).getOrNull()
    }

    protected open val findOneTransaction: Transaction<SyncDao<T>, Query, T?> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, Query, T?>()
            .main { delegate, context -> delegate.findOne(context.input!!) }
            .type(TransactionType.READ)
            .precedence(TransactionPrecedence.ANY)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun findOne(query: Query): T? {
        val context = findOneTransaction.toContext(delegates)
        val input = query
        return handler(context, input).getOrNull()
    }

    protected open val saveTransaction: Transaction<SyncDao<T>, Pair<String, T?>, T?> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, Pair<String, T?>, T?>()
            .initialValue { context ->
                val (id, _) = context.input!!
                id to findById(id)
            }
            .main { delegate, context ->
                val (id, entity) = context.input!!
                delegate.save(id, entity!!)
            }
            .type(TransactionType.UPDATE)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun save(id: String, entity: T): T? {
        val context = saveTransaction.toContext(delegates)
        val input = id to entity
        return handler(context, input).getOrNull()
    }

    protected open val saveBatchTransaction by lazy {
        // TODO: Figure out ID mapping for different dao's
        val transaction = Transaction.Builder<SyncDao<T>, List<T>, List<T>>()
            .initialValue { context ->
                val delegate: AbstractSyncDao<T>? = context.delegates.firstNotNullOfOrNull { it as? AbstractSyncDao<T> }
                val initial = context.input!!.mapNotNull { entity ->
                    val id = delegate?.idMapper?.getId(entity)
                    id?.let { findById(it) }
                }
                initial
            }
            .main { delegate, context ->
                delegate.saveBatch(context.input!!)
            }
            .type(TransactionType.UPDATE)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun saveBatch(entities: List<T>): List<T> {
        val context = saveBatchTransaction.toContext(delegates)
        val input = entities
        return handler(context, input).getOrThrow() ?: entities
    }

    protected open val upsertTransaction: Transaction<SyncDao<T>, Pair<String, T?>, T?> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, Pair<String, T?>, T?>()
            .initialValue { context ->
                val (id, _) = context.input!!
                id to findById(id)
            }
            .main { delegate, context ->
                val (id, entity) = context.input!!
                delegate.upsert(id, entity!!)
            }
            .type(TransactionType.UPDATE)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun upsert(id: String, entity: T): T? {
        val context = upsertTransaction.toContext(delegates)
        val input = id to entity
        return handler(context, input).getOrNull()
    }

    protected open val upsertBatchTransaction by lazy {
        // TODO: Figure out ID mapping for different dao's
        val transaction = Transaction.Builder<SyncDao<T>, List<T>, List<T>>()
            .initialValue { context ->
                val delegate: AbstractSyncDao<T>? = context.delegates.firstNotNullOfOrNull { it as? AbstractSyncDao<T> }
                val initial = context.input!!.mapNotNull { entity ->
                    val id = delegate?.idMapper?.getId(entity)
                    id?.let { findById(it) }
                }
                initial
            }
            .main { delegate, context ->
                delegate.upsertBatch(context.input!!)
            }
            .type(TransactionType.UPDATE)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun upsertBatch(entities: List<T>): List<T> {
        val context = upsertBatchTransaction.toContext(delegates)
        val input = entities
        return handler(context, input).getOrThrow() ?: entities
    }

    protected open val closeTransaction: Transaction<SyncDao<T>, Nothing, Unit?> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, Nothing, Unit?>()
            .main { delegate, _ ->
                delegate.close()
            }
            .type(TransactionType.IO)
            .precedence(TransactionPrecedence.ALL)
            .onStateChange(this::onStateChange)
            .build()
        transaction
    }

    override fun close() {
        val context = closeTransaction.toContext(delegates)
        val input = null
        handler(context, input).getOrNull()
    }

}
