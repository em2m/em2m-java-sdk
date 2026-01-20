package io.em2m.transactions

enum class TransactionState {
    CREATED,
    INITIALIZED,
    RUNNING,
    SUCCESS,
    FAILURE,
    COMPLETED
}
