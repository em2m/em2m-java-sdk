package io.em2m.transactions

open class TransactionHandler {

    private val transactionMap: MutableMap<Class<*>, MutableSet<Transaction<*, *, *>>> = mutableMapOf()
    private val listeners = mutableSetOf<OnStateChangeListener>()

    fun forClass(clazz: Class<*>, permissive: Boolean = true): Set<Transaction<*,*,*>> {
        val ret = transactionMap.getOrDefault(clazz, emptySet()).toMutableSet()

        if (permissive) {
            transactionMap.forEach { (key: Class<*>, transactions: MutableSet<Transaction<*,*,*>>) ->
                if (key == clazz) return@forEach

                if (key.isAssignableFrom(clazz)) {
                    ret.addAll(transactions)
                }
            }
        }

        return ret
    }

    private fun forObj(obj: Any, permissive: Boolean = true): Set<Transaction<*,*,*>> {
        return if (obj is Class<*>) {
            forClass(obj, permissive=permissive)
        } else {
            forClass(obj.javaClass, permissive=permissive)
        }
    }

    fun <T, INPUT: Any, OUTPUT> transact(t: T, transaction: Transaction<T, INPUT, OUTPUT>) = apply {
        this.transact((t as Any).javaClass as Class<T>, transaction)
    }

    fun <T, INPUT: Any, OUTPUT> transact(clazz: Class<T>, transaction: Transaction<T, INPUT, OUTPUT>) = apply {
        transactionMap.putIfAbsent(clazz, mutableSetOf())?.add(transaction)
    }

    fun onStateChange(allowedStates: Collection<TransactionState> = TransactionState.entries, fn: (TransactionContext<*, *, *>) -> Unit) {
        listeners.add(DelegateOnStateChangeListener(allowedStates = allowedStates.toTypedArray(), fn=fn))
    }

    fun onCreate(fn: (TransactionContext<*, *, *> ) -> Unit) = this.onStateChange(listOf(TransactionState.CREATED), fn)

    fun onInitialized(fn: (TransactionContext<*, *, *> ) -> Unit) = this.onStateChange(listOf(TransactionState.INITIALIZED), fn)

    fun onFailure(fn: (TransactionContext<*, *, *> ) -> Unit) = this.onStateChange(listOf(TransactionState.FAILURE), fn)

    fun onSuccess(fn: (TransactionContext<*, *, *> ) -> Unit) = this.onStateChange(listOf(TransactionState.SUCCESS), fn)

    fun updateState(context: TransactionContext<*, *, *>, transaction: Transaction<*, *, *>, state: TransactionState) {
        transaction.state = state

        context.safeTry { transaction.onStateChange(context) }
        listeners.filter { it.matches(state) }
            .forEach { context.safeTry { it.onStateChange(context) } }
    }

    open fun getTransactionPriority(delegate: Any?, context: TransactionContext<*, *, *>): Int {
        return TransactionType.MEDIUM_PRIORITY
    }

    @Throws(TransactionException::class)
    operator fun <DELEGATE, INPUT : Any, OUTPUT> invoke(_context: TransactionContext<DELEGATE, INPUT, OUTPUT>): Result<OUTPUT> {
        val internalTransactions = if (_context.validTransaction) {
            setOf(_context.transaction)
        } else {
            emptySet()
        }
        val transactionsForDelegate = forClass(_context.`class`).mapNotNull { transaction ->
            transaction as? Transaction<DELEGATE, INPUT, OUTPUT>
        }.union(internalTransactions)

        val contextsToTransactions = transactionsForDelegate.associateBy { transaction ->
            _context.copy().apply { this.transaction = transaction }
        }

        // TODO: add multi-threaded transactions
        contextsToTransactions.forEach { (context, transaction) ->
            updateState(context, transaction, TransactionState.CREATED)
            updateState(context, transaction, TransactionState.INITIALIZED)

            // cond
            context.condition = context.tryOr(false) { transaction.condition(context) }
            if (context.condition != true) {
                return Result.failure(TransactionException("Condition failed", context))
            }
            // initial
            context.initial = context.tryOrThrow { transaction.initialValue(context) }

            updateState(context, transaction, TransactionState.RUNNING)
            context.output = context.tryOrNull {
                // TODO: Add sorting
                val sorted = context.delegates.sortedBy { delegate ->
                    getTransactionPriority(delegate, context)
                }
                val results = context.delegates.map { delegate ->
                    transaction.run(delegate, context) as OUTPUT
                }
                context.transaction.combine(results)
            }

            if (context.success) {
                updateState(context, transaction, TransactionState.SUCCESS)
            } else {
                // catch
                updateState(context, transaction, TransactionState.FAILURE)
            }

            // finally
            updateState(context, transaction, TransactionState.COMPLETED)
        }

        return Result.success(_context.output!!)

    }



}
