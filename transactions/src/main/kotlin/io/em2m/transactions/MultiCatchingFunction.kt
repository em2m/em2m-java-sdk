package io.em2m.transactions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.utils.MultiException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.sortedBy

open class MultiCatchingFunction<ELEM>(vararg delegates: ELEM?,
                                       val objectMapper: ObjectMapper = jacksonObjectMapper(),
                                       val operatorComparator: ((ELEM, OperationType) -> Int)? = null){

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)
    protected val delegates: Collection<ELEM> = delegates.filterNotNull()
    // TODO: Figure out sorting
    private val sortedOperations: Map<OperationType, List<ELEM>> = run {
        return@run if (operatorComparator != null) {
            OperationType.entries.associateWith { type ->
                this.delegates.sortedBy { elem -> operatorComparator!!.invoke(elem, type) }
            }
        } else {
            OperationType.entries.associateWith { this.delegates.toList() }
        }
    }
    val primary: ELEM = requireNotNull(this.delegates.firstOrNull()) {
        "No primary delegate declared."
    }

    protected operator fun get(type: OperationType): List<ELEM>? {
        return sortedOperations[type]
    }

    fun <IN, OUT> OperationFromTransaction(type: OperationType, precedence: OperationPrecedence, transaction: Transaction<ELEM, IN, OUT>): Operation<IN, OUT> {

        val condition = { elem: ELEM, input: IN -> elem.runCatching {
            transaction.condition(elem, input)
        } }
        val tryFn = transaction::run
        val undoable = transaction as? Undoable<ELEM, IN, OUT>
        val undoFn = undoable?.let { it::undo }
        val finallyFn = undoable?.let { it::finally }
        val initialStateFn = undoable?.let { it::initialState }

        val retryable = transaction as? Retryable
        val retryAction = retryable?.let { RetryOnFailure<ELEM, IN>(it.limit) }
        val onFailure = OnFailure<ELEM, IN, OUT>(
            undoAction = undoFn,
            retryAction = retryAction
        )

        val combinable = transaction as? Combinable<ELEM,IN>
        val combineFn = combinable?.let { it::combine } as? ((List<Pair<ELEM, OUT?>>) -> OUT)

        return this.Operation(type, precedence, condition, tryFn, onFailure, finallyFn, combineFn, initialStateFn) as MultiCatchingFunction<ELEM>.Operation<IN, OUT>


    }

    inner class Operation<IN, OUT>(val type: OperationType,
                                   val precedence: OperationPrecedence,
                                   val condition: ((ELEM, IN) -> Result<Boolean>)? = null,
                                   val tryFn: (ELEM, IN) -> Any?,
                                   val onFailure: OnFailure<ELEM, IN, OUT> = OnFailure(),
                                   val finallyFn: ((ELEM, IN) -> Any?)? = null,
                                   val combineFn: ((List<Pair<ELEM, OUT?>>) -> OUT)? = null,
                                   val initialStateFn: (() -> IN?)? = null) {

        private var initialState: IN? = null

        private fun retryOnFailure(elem: ELEM, param: IN, result: Result<*>): Result<*> {
            return if (result.isFailure) {
                onFailure.retry(elem, param, tryFn, result.exceptionOrNull())
            } else {
                result
            }
        }

        private fun undoOnFailure(elem: ELEM, param: IN): Result<Any?> {
            return onFailure.undo(elem, param, initialState)
        }

        operator fun invoke(param: IN, debug: Boolean = false): OUT? {
            // cond
            if (condition != null) {
                val conditionResult = delegates.all { delegate ->
                    val r = condition!!.invoke(delegate, param)
                    val throwable = r.exceptionOrNull()
                    if (r.isFailure && throwable != null) {
                        logger.error("Condition failed! Not invoking.", throwable)
                        return@all false
                    }
                    r.getOrElse { false }
                }
                if (!conditionResult) {
                    logger.error("Condition failed! Not invoking.")
                    return null
                }
            }

            if(onFailure.requiresInitialState) {
                this.initialState = runCatching { initialStateFn?.invoke() }.getOrNull()
            }
            val operationDelegates = sortedOperations[type] ?: return null
            val delegatePairs = mutableListOf<Pair<ELEM, Result<Any?>>>()

            // try
            for(index in operationDelegates.indices) {
                val delegate = operationDelegates[index]
                var result: Result<*> = delegate.runCatching {
                    tryFn(delegate, param)
                }.onFailure { exception ->
                    if (debug) {
                        logger.error("Exception in delegate invocation!", exception)
                    }
                }
                // retry failed operations
                result = this.retryOnFailure(delegate, param, result)
                // early escape, don't do this for mutations since that could lead to unstable data parity
                if (!type.mutation && result.isSuccess && precedence == OperationPrecedence.ANY) {
                    return result.getOrNull() as? OUT
                }
                delegatePairs.add(delegate to result)
            }
            var (success, errors) = delegatePairs
                .partition { (_, result) -> result.isSuccess }

            // combine results
            val ret: OUT? = if (errors.isEmpty() && precedence == OperationPrecedence.ANY ) {
                delegatePairs.firstOrNull()?.second?.getOrNull() as? OUT
            } else if (errors.isEmpty() && precedence == OperationPrecedence.ALL){
                val values = delegatePairs.map { (delegate, result) ->
                    delegate to result.getOrNull() as? OUT
                }
                if (combineFn == null && debug) {
                    logger.error("Combine Function isn't set! This is undefined behavior for OperationPrecedence.ANY")
                }
                if (values.size == 1) {
                    values.firstOrNull()?.second as? OUT
                } else {
                    combineFn?.invoke(values)
                }
            } else {
                null
            }

            // catch
            var exceptions = errors.mapNotNullTo(mutableSetOf()) { (_, result) -> result.exceptionOrNull() }
            if (errors.isNotEmpty() && onFailure.hasUndo) {
                if (debug) {
                    val multiError = MultiException(errors = exceptions.toTypedArray())
                    logger.error(multiError.message, multiError)
                }
                exceptions = mutableSetOf()

                val undoSubjects = when(onFailure.undoStrategy) {
                    UndoStrategy.SUCCESS -> success
                    UndoStrategy.FAILURE -> errors
                    else -> delegatePairs
                }
                undoSubjects.forEach { (delegate, _) ->
                    undoOnFailure(delegate, param).onFailure { exception ->
                        exceptions.add(exception)
                        if (debug) {
                            logger.error("There was an error undoing the operation!", exception)
                        }
                    }
                }
            }
            // finally
            delegatePairs.forEach { (delegate, _) ->
                delegate.runCatching {
                    finallyFn?.invoke(delegate, param)
                }.onFailure { exception ->
                    exceptions.add(exception)
                    if (debug) {
                        logger.error("Exception in finally block!", exception)
                    }
                }
            }
            if (exceptions.isNotEmpty()) {
                throw MultiException(errors=exceptions.toTypedArray())
            }

            return ret
        }

    }

}
