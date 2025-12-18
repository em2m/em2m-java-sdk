package io.em2m.transactions

import io.em2m.utils.OperationType

open class TransactionHandler(val config: Map<Class<*>, TransactionConfig> = mutableMapOf()) : AbstractTransactionListener() {

    private val transactionMap: MutableMap<Class<*>, MutableSet<Transaction<*, *, *>>> = mutableMapOf()

    private fun forClass(clazz: Class<*>, permissive: Boolean = true): Set<Transaction<*,*,*>> {
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

    open fun getPriority(delegate: Any?, context: TransactionContext<*, *, *>): Int {
        return OperationType.MEDIUM_PRIORITY
    }

    fun updateState(context: TransactionContext<*, *, *>, transaction: Transaction<*, *, *>, state: TransactionState) {
        transaction.state = state

        context.safeTry { transaction.onStateChange(context) }
        this.onStateChange(context)
    }

    @Throws(TransactionException::class)
    operator fun <DELEGATE, INPUT : Any, OUTPUT> invoke(_context: TransactionContext<DELEGATE, INPUT, OUTPUT>, input: INPUT? = null): Result<OUTPUT?> {
        val internalTransactions = setOf(_context.transaction)
        val transactionsForDelegate = forClass(_context.clazz).mapNotNull { transaction ->
            transaction as? Transaction<DELEGATE, INPUT, OUTPUT>
        }.union(internalTransactions)

        val contextsToTransactions = transactionsForDelegate.associateBy { transaction ->
            _context.copy().apply { this.transaction = transaction }
        }

        // TODO: add multi-threaded transactions
        val totalResults = mutableListOf<OUTPUT>()
        contextsToTransactions.forEach { (context, transaction) ->
            updateState(context, transaction, TransactionState.CREATED)
            updateState(context, transaction, TransactionState.INITIALIZED)

            // cond
            context.condition = context.tryOr(false) {
                context.delegates.all { delegate -> transaction.condition(delegate, context) }
            }
            if (context.condition != true) {
                val exception = TransactionException("Condition failed", context)
                context.errors.add(exception)
                return Result.failure(exception)
            }

            // input
            if (input != null) {
                context.input = input
            }

            // initial
            context.initial = context.tryOrThrow { transaction.initialValue(context) }

            updateState(context, transaction, TransactionState.RUNNING)
            context.output = context.tryOrNull {
                val sorted = context.delegates.sortedBy { delegate ->
                    getPriority(delegate, context)
                }
                val results = sorted.map { delegate ->
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
        contextsToTransactions.values.forEach { transaction ->
            try {
                _context.output = transaction.combine(totalResults)
            } catch (ex: Exception) {
                System.err.println("TransactionHandler exception: ${ex.message}")
            }
        }

        return Result.success(_context.output)

    }



}
