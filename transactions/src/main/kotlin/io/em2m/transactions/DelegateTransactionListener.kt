package io.em2m.transactions

class DelegateTransactionListener(allowedStates: Collection<TransactionState> = TransactionState.entries, fn: TransactionListener): TransactionListener by fn {

    private val allowedStates = allowedStates.toSet()

    override fun matches(state: TransactionState): Boolean = state in allowedStates

}
