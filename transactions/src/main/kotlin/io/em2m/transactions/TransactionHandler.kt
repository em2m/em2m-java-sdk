package io.em2m.transactions

import io.em2m.utils.OperationType
import org.slf4j.LoggerFactory

open class TransactionHandler(val config: Map<Class<*>, TransactionConfig> = mutableMapOf(),
                              private var sortedWith: ((delegate: Any?, context: TransactionContext<*, *, *>) -> Int)? = null) : AbstractTransactionListener() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val transactionMap: MutableMap<Class<*>, MutableSet<Transaction<*, *, *>>> = mutableMapOf()

    fun getConfigForClass(clazz: Class<*>, permissive: Boolean = true): TransactionConfig? {
        val ret = mutableSetOf(config[clazz])

        if (permissive) {
            config.forEach { (key: Class<*>, config: TransactionConfig) ->
                if (key == clazz) return@forEach

                if (key.isAssignableFrom(clazz)) {
                    ret.add(config)
                }
            }
        }

        return ret.filterNotNull().firstOrNull()
    }

    private fun getConfigForObj(obj: Any, permissive: Boolean = true): TransactionConfig? {
        return if (obj is Class<*>) {
            getConfigForClass(obj, permissive= permissive)
        } else {
            getConfigForClass(obj.javaClass, permissive= permissive)
        }
    }

    private fun forClass(clazz: Class<*>, permissive: Boolean = true): Set<Transaction<*,*,*>> {
        val ret = transactionMap.getOrDefault(clazz, emptySet()).toMutableSet()

        // allow parent class
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

    fun sortedWith(fn: ((delegate: Any?, context: TransactionContext<*, *, *>) -> Int)?) = apply {
        this.sortedWith = fn
    }

    open fun getPriority(delegate: Any?, context: TransactionContext<*, *, *>): Int {
        return sortedWith?.invoke(delegate, context) ?: OperationType.MEDIUM_PRIORITY
    }

    private fun getTransactionPriority(delegate: Any?, context: TransactionContext<*,*,*>): Int {
        return sortedWith?.invoke(delegate, context) ?: this.getPriority(delegate, context)
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
            val config = getConfigForClass(_context.clazz) ?: TransactionConfig.DEFAULT
            _context.copy().apply { this.transaction = transaction; this.config = config}
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
                    getTransactionPriority(delegate, context)
                }
                val results: List<OUTPUT?> = sorted.map { delegate ->
                    val clazz = (delegate as Any).javaClass
                    val config = getConfigForClass(clazz, permissive = false) ?: context.config
                    try {
                        transaction.run(delegate, context) as OUTPUT
                    } catch (ex : Exception) {
                        when (config.errorStrategy) {
                            TransactionErrorStrategy.ALWAYS -> { throw ex }
                            TransactionErrorStrategy.LOG -> {
                                logger.error("Transaction error!", ex)
                                null
                            }
                            else -> {
                                null
                            }
                        }
                    }
                }
                context.transaction.combine(results)
            }
            context.output?.let { totalResults.add(it) }

            if (context.success) {
                updateState(context, transaction, TransactionState.SUCCESS)
            } else {
                // catch
                updateState(context, transaction, TransactionState.FAILURE)
            }

            // finally
            updateState(context, transaction, TransactionState.COMPLETED)
        }
        if (totalResults.size > 1) {
            contextsToTransactions.values.forEach { transaction ->
                try {
                    _context.output = transaction.combine(totalResults)
                } catch (ex: Exception) {
                    System.err.println("TransactionHandler exception: ${ex.message}")
                }
            }
        } else {
            try {
                _context.output = totalResults.firstOrNull()
            } catch (ex: Exception) {
                System.err.println("TransactionHandler exception: ${ex.message}")
            }
        }


        return Result.success(_context.output)

    }



}
