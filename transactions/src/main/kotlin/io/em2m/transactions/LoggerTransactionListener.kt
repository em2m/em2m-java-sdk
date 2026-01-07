package io.em2m.transactions

import io.em2m.utils.MultiException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LoggerTransactionListener(
    matches: List<TransactionState> = TransactionState.entries,
    val logger: Logger = LoggerFactory.getLogger(LoggerTransactionListener::class.java))
    : AbstractTransactionListener(matches) {

    override fun onStateChange(context: TransactionContext<*, *, *>) {
        when(context.transaction.state) {
            TransactionState.FAILURE -> {
                val exception = MultiException(errors= context.errors.toTypedArray())
                logger.error("Transaction failed.", exception)
            }
            else -> {
                if (context.debug) {
                    logger.debug("Transaction state updated to {}", context.transaction.state)
                }
            }
        }
    }

}
