package io.em2m.transactions

fun interface TransactionListener {

    fun matches(state: TransactionState): Boolean = true

    fun onStateChange(context: TransactionContext<*, *, *>)

}
