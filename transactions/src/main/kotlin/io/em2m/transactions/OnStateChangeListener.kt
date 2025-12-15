package io.em2m.transactions

fun interface OnStateChangeListener {

    fun matches(state: TransactionState): Boolean = true

    fun onStateChange(context: TransactionContext<*, *, *>)

}
