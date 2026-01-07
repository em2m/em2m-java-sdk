package io.em2m.transactions

open class AbstractTransactionListener(states: Collection<TransactionState> = TransactionState.entries) : TransactionListener {

    constructor(vararg states: TransactionState): this(states.toList())

    protected open val listeners = mutableSetOf<TransactionListener>()

    protected open val allowedStates = states.toSet()

    override fun matches(state: TransactionState): Boolean = state in allowedStates

    override fun onStateChange(context: TransactionContext<*, *, *>) {
        val state = context.transaction.state
        listeners.filter { it.matches(state) }
            .forEach { context.safeTry { it.onStateChange(context) } }
    }

    fun addListener(listener: TransactionListener) = apply { this.listeners.add(listener) }

    fun removeListener(listener: TransactionListener) = apply { this.listeners.remove(listener) }

    fun onStateChange(allowedStates: Collection<TransactionState> = TransactionState.entries, fn: TransactionListener) {
        listeners.add(DelegateTransactionListener(allowedStates = allowedStates, fn=fn))
    }

    fun onCreate(fn: TransactionListener) = this.onStateChange(listOf(TransactionState.CREATED), fn)

    fun onInitialized(fn: TransactionListener) = this.onStateChange(listOf(TransactionState.INITIALIZED), fn)

    fun onFailure(fn: TransactionListener) = this.onStateChange(listOf(TransactionState.FAILURE), fn)

    fun onSuccess(fn: TransactionListener) = this.onStateChange(listOf(TransactionState.SUCCESS), fn)

}
