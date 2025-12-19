package io.em2m.transactions

data class TransactionConfig(
    var errorStrategy: TransactionErrorStrategy,
    val properties: Map<String, Any?> = mutableMapOf()) {

    companion object {
        val DEFAULT = TransactionConfig(TransactionErrorStrategy.ALWAYS)
    }

}
