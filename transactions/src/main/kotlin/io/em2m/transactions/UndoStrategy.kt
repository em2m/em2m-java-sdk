package io.em2m.transactions

enum class UndoStrategy {
    SUCCESS, FAILURE, ALL;

    companion object {
        val DEFAULT = ALL
    }

}
