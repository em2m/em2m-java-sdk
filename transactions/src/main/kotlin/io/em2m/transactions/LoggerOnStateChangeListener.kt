package io.em2m.transactions

import io.em2m.utils.MultiException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LoggerOnStateChangeListener(
    matches: List<TransactionState> = TransactionState.entries,
    val logger: Logger = LoggerFactory.getLogger(LoggerOnStateChangeListener::class.java))
    : AbstractOnStateChangeListener(matches) {

    override fun onStateChange(context: TransactionContext<*, *, *>) {
        when(context.transaction.state) {
            TransactionState.FAILURE -> {
                val exception = MultiException(errors= context.errors.toTypedArray())
                logger.error("Transaction failed.", exception)
            }
            else -> {
                logger.debug("Transaction state updated to {}", context.transaction.state)
            }
        }
    }

}
