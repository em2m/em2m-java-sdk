package io.em2m.utils

enum class OperationType(val mutation: Boolean = true) {
    CREATE,
    READ(false),
    SEARCH(false),
    UPDATE,
    DELETE,
    IO;

    companion object {
        const val HIGH_PRIORITY = 0
        const val MEDIUM_PRIORITY = 50
        const val LOW_PRIORITY = 100
    }
}
