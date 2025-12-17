package io.em2m.transactions

data class TransactionConfig(
    val strategy: TransactionErrorStrategy,
    val properties: Map<String, Any?> = mutableMapOf())
