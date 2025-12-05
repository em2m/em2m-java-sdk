package io.em2m.transactions

import io.em2m.utils.retry

class OnFailure<ELEM, IN> (private val undoAction: UndoOnFailureAction<ELEM, IN>? = null,
                           val undoStrategy: UndoStrategy = UndoStrategy.DEFAULT,
                           private val retryAction: RetryOnFailure<ELEM, IN>? = null){

    fun undo(elem: ELEM,
             param: IN,
             initial: IN?): Result<Any?> {
        return if (undoAction != null && initial != null) {
            runCatching {
                this.undoAction!!.invoke(elem, param, initial)
            }
        } else {
            Result.success(null)
        }
    }

    val hasUndo: Boolean = undoAction != null

    fun retry(elem: ELEM,
              param: IN,
              operation: (ELEM, IN) -> Any?,
              throwable: Throwable?): Result<Any?> {
        return if (throwable == null) {
            Result.success(null)
        } else if (retryAction != null) {
            this.retryAction(elem, param, operation, throwable)
        } else {
            Result.failure(throwable)
        }
    }

    val hasRetry: Boolean = retryAction != null

    val requiresInitialState: Boolean = hasUndo

}

class RetryOnFailure<ELEM, IN>(val limit: Int) {

    operator fun invoke(elem: ELEM, param: IN, operation: (ELEM, IN) -> Any?, throwable: Throwable?): Result<Any?> {
        return runCatching {
            retry(limit) {
                operation(elem, param)
            }
        }
    }

}

fun interface UndoOnFailureAction<ELEM, IN> {
    operator fun invoke(
        elem: ELEM,
        param: IN,
        initial: IN
    ): Any?
}
