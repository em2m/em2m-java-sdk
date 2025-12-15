package io.em2m.transactions

class DelegateOnStateChangeListener: AbstractOnStateChangeListener {

    private val fn: (TransactionContext<*, *, *>) -> Unit

    constructor(vararg allowedStates: TransactionState, fn: (TransactionContext<*, *, *>) -> Unit): super(allowedStates.toList()) {
        this.fn = fn
    }

    override fun onStateChange(context: TransactionContext<*, *, *>) = fn(context)

}
