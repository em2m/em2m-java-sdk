package io.em2m.obj

enum class UndoStrategy {
    SUCCESS, FAILURE, ALL;

    companion object {
        val DEFAULT = ALL
    }

}
