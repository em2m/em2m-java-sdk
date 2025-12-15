package io.em2m.transactions

abstract class AbstractOnStateChangeListener(states: Collection<TransactionState> = TransactionState.entries) : OnStateChangeListener {

    protected open val allowedStates = states.toSet()

    override fun matches(state: TransactionState): Boolean = state in allowedStates

}
