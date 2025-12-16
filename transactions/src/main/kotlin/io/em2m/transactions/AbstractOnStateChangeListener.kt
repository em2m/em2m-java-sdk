package io.em2m.transactions

abstract class AbstractOnStateChangeListener(states: Collection<TransactionState> = TransactionState.entries) : OnStateChangeListener {

    constructor(vararg states: TransactionState): this(states.toList())

    protected open val allowedStates = states.toSet()

    override fun matches(state: TransactionState): Boolean = state in allowedStates

}
