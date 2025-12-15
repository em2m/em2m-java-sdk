package io.em2m.search.core.daos

import io.em2m.search.core.model.Query
import io.em2m.search.core.model.SearchRequest
import io.em2m.search.core.model.SearchResult
import io.em2m.search.core.model.SyncDao
import io.em2m.transactions.*

open class TransactionDao<T : Any, DAO>(val delegates: List<DAO>): SyncDao<T> where DAO : SyncDao<T> {

    protected val handler = object : TransactionHandler() {
        override fun getTransactionPriority(delegate: Any?, context: TransactionContext<*, *, *>): Int {
            if (delegate is SyncDao<*>) {
                return delegate.getTransactionPriority(context.transaction.type)
            }
            return super.getTransactionPriority(delegate, context)
        }
    }

    private val listeners: MutableSet<OnStateChangeListener> = mutableSetOf()

    protected open val createTransaction: Transaction<SyncDao<T>, T, T?> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, T, T?>()
            .main { delegate, context -> delegate.create(context.input!!) }
            .onStateChange(::updateListeners)
            .type(TransactionType.CREATE)
            .precedence(TransactionPrecedence.ALL)
            .build()
        transaction
    }
    override fun create(entity: T): T? {
        val context = TransactionContext<SyncDao<T>, T, T?>(delegates= delegates)
        context.transaction = createTransaction
        return handler(context).getOrNull()
    }

    protected val deleteByIdTransaction : Transaction<SyncDao<T>, Pair<String, T?>, Boolean> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, Pair<String, T?>, Boolean>()
            .initialValue { context ->
                val (id, _) = context.input!!
                id to findById(id)
            }
            .main { delegate, context -> delegate.deleteById(context.input!!.first) }
            .onStateChange(::updateListeners)
            .type(TransactionType.DELETE)
            .precedence(TransactionPrecedence.ALL)
            .build()
        transaction
    }

    override fun deleteById(id: String): Boolean {
        val context = TransactionContext<SyncDao<T>, Pair<String, T?>, Boolean>(delegates= delegates)
        context.transaction = deleteByIdTransaction
        return handler(context).getOrDefault(false)
    }

    protected open val existsTransaction: Transaction<SyncDao<T>, String, Boolean> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, String, Boolean>()
            .main { delegate, context -> delegate.exists(context.input!!) }
            .onStateChange(::updateListeners)
            .type(TransactionType.READ)
            .precedence(TransactionPrecedence.ANY)
            .build()
        transaction
    }

    override fun exists(id: String): Boolean {
        val context = TransactionContext<SyncDao<T>, String, Boolean>(delegates=delegates)
        context.transaction = existsTransaction
        return handler(context).getOrDefault(false)
    }

    protected open val searchTransaction: Transaction<SyncDao<T>, SearchRequest, SearchResult<T>> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, SearchRequest, SearchResult<T>>()
            .main { delegate, context ->
                delegate.search(context.input!!)
            }
            .onStateChange(::updateListeners)
            .type(TransactionType.SEARCH)
            .precedence(TransactionPrecedence.ANY)
            .combine { SearchResult.combineSearchResults(it) }
            .build()
        transaction
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        val context = TransactionContext<SyncDao<T>, SearchRequest, SearchResult<T>>(delegates=delegates)
        context.transaction = searchTransaction
        return handler(context).getOrThrow()
    }

    protected open val countTransaction: Transaction<SyncDao<T>, Query, Long> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, Query, Long>()
            .main { delegate, context -> delegate.count(context.input!!) }
            .onStateChange(::updateListeners)
            .type(TransactionType.SEARCH)
            .precedence(TransactionPrecedence.ANY)
            .build()
        transaction
    }

    override fun count(query: Query): Long {
        val context = TransactionContext<SyncDao<T>, Query, Long>(delegates= delegates)
        context.transaction = countTransaction
        return handler(context).getOrDefault(-1L)
    }

    protected open val findByIdTransaction: Transaction<SyncDao<T>, String, T?> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, String, T?>()
            .main { delegate, context -> delegate.findById(context.input!!) }
            .onStateChange(::updateListeners)
            .type(TransactionType.READ)
            .precedence(TransactionPrecedence.ANY)
            .build()
        transaction
    }

    override fun findById(id: String): T? {
        val context = TransactionContext<SyncDao<T>, String, T?>(delegates= delegates)
        context.transaction = findByIdTransaction
        return handler(context).getOrNull()
    }

    protected open val findOneTransaction: Transaction<SyncDao<T>, Query, T?> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, Query, T?>()
            .main { delegate, context -> delegate.findOne(context.input!!) }
            .onStateChange(::updateListeners)
            .type(TransactionType.READ)
            .precedence(TransactionPrecedence.ANY)
            .build()
        transaction
    }

    override fun findOne(query: Query): T? {
        val context = TransactionContext<SyncDao<T>, Query, T?>(delegates= delegates)
        context.transaction = findOneTransaction
        return handler(context).getOrNull()
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
            .onStateChange(::updateListeners)
            .type(TransactionType.UPDATE)
            .precedence(TransactionPrecedence.ALL)
            .build()
        transaction
    }

    override fun save(id: String, entity: T): T? {
        val context = TransactionContext<SyncDao<T>, Pair<String, T?>, T?>(delegates= delegates)
        context.transaction = saveTransaction
        return handler(context).getOrNull()
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
            .onStateChange(::updateListeners)
            .type(TransactionType.UPDATE)
            .precedence(TransactionPrecedence.ALL)
            .build()
        transaction
    }

    override fun saveBatch(entities: List<T>): List<T> {
        val context = TransactionContext<SyncDao<T>, List<T>, List<T>>(delegates= delegates)
        context.transaction = saveBatchTransaction
        return handler(context).getOrThrow()
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
            .onStateChange(::updateListeners)
            .type(TransactionType.UPDATE)
            .precedence(TransactionPrecedence.ALL)
            .build()
        transaction
    }

    override fun upsert(id: String, entity: T): T? {
        val context = TransactionContext<SyncDao<T>, Pair<String, T?>, T?>(delegates= delegates)
        context.transaction = upsertTransaction
        return handler(context).getOrNull()
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
            .onStateChange(::updateListeners)
            .type(TransactionType.UPDATE)
            .precedence(TransactionPrecedence.ALL)
            .build()
        transaction
    }

    override fun upsertBatch(entities: List<T>): List<T> {
        val context = TransactionContext<SyncDao<T>, List<T>, List<T>>(delegates= delegates)
        context.transaction = upsertBatchTransaction
        return handler(context).getOrThrow()
    }

    protected open val closeTransaction: Transaction<SyncDao<T>, Nothing, Unit?> by lazy {
        val transaction = Transaction.Builder<SyncDao<T>, Nothing, Unit?>()
            .main { delegate, _ ->
                delegate.close()
            }
            .onStateChange(::updateListeners)
            .type(TransactionType.IO)
            .precedence(TransactionPrecedence.ALL)
            .build()
        transaction
    }

    override fun close() {
        val context = TransactionContext<SyncDao<T>, Nothing, Unit?>(delegates= delegates)
        context.transaction = closeTransaction
        handler(context).getOrNull()
    }

    private fun updateListeners(context: TransactionContext<*,*,*>) {
        val state = context.transaction.state
        listeners.filter { it.matches(state) }
            .forEach { context.safeTry { it.onStateChange(context) } }
    }

    fun onStateChange(allowedStates: Collection<TransactionState> = TransactionState.entries, fn: (TransactionContext<*, *, *>) -> Unit) {
        listeners.add(DelegateOnStateChangeListener(allowedStates = allowedStates.toTypedArray(), fn=fn))
    }

    fun onCreate(fn: (TransactionContext<*, *, *> ) -> Unit) = this.onStateChange(listOf(TransactionState.CREATED), fn)

    fun onInitialized(fn: (TransactionContext<*, *, *> ) -> Unit) = this.onStateChange(listOf(TransactionState.INITIALIZED), fn)

    fun onFailure(fn: (TransactionContext<*, *, *> ) -> Unit) = this.onStateChange(listOf(TransactionState.FAILURE), fn)

    fun onSuccess(fn: (TransactionContext<*, *, *> ) -> Unit) = this.onStateChange(listOf(TransactionState.SUCCESS), fn)

}
