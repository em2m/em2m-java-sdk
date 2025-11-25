package io.em2m.obj

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.utils.FallbackPair
import io.em2m.utils.MultiException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class MultiCatchingFunctions<ELEM1, ELEM2>(delegates1: List<ELEM1>,
                                                delegate1Class: Class<*>? = null,
                                                delegates2: List<ELEM2>,
                                                delegate2Class: Class<*>? = null,
                                                val objectMapper: ObjectMapper = jacksonObjectMapper(),
                                                val operatorComparator1: ((ELEM1, OperationType) -> Int)? = null,
                                                val operatorComparator2: ((ELEM2, OperationType) -> Int)? = null) {

    constructor(fallbackPair: FallbackPair<ELEM1, ELEM2>,
        objectMapper: ObjectMapper = jacksonObjectMapper()) : this(
        delegates1 = listOf(fallbackPair.primary),
        delegates2 = fallbackPair.fallbacks,
        objectMapper= objectMapper,
        operatorComparator1 = { _, _ -> Int.MIN_VALUE},
        operatorComparator2 = { _, _ -> Int.MAX_VALUE}
    )

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val delegates1 = delegates1.filterNotNull()
    private val delegates2 = delegates2.filterNotNull()
    private val allDelegates = mutableListOf<Any>().apply {
        addAll(this@MultiCatchingFunctions.delegates1.map { it as Any })
        addAll(this@MultiCatchingFunctions.delegates2.map { it as Any })
    }

    val primary: Any = requireNotNull(this.allDelegates.firstOrNull()) {
        "No primary delegate declared."
    }

    private val delegate1Class = delegate1Class ?: delegates1.firstOrNull()?.javaClass
    private val delegate2Class = delegate2Class ?: delegates2.firstOrNull()?.javaClass

    private val Any?.isDelegate1: Boolean
        get() = run {
            if (this == null) return false
            if (delegate1Class == null) return this as? ELEM1 != null
            return delegate1Class.isAssignableFrom(this@run.javaClass)
        }

    private val Any?.isDelegate2: Boolean
        get() = run {
            if (this == null) return false
            if (delegate2Class == null) return this as? ELEM2 != null
            return delegate2Class.isAssignableFrom(this@run.javaClass)
        }

    private val comparator: ((Any, OperationType) -> Int) = run {
        object : Function2<Any, OperationType, Int> {
            override fun invoke(p1: Any, p2: OperationType): Int {
                val ret = if (p1.isDelegate1) {
                    operatorComparator1?.invoke(p1 as ELEM1, p2)
                } else if (p1.isDelegate2) {
                    operatorComparator2?.invoke(p1 as ELEM2, p2)
                } else {
                    null
                }
                return ret ?: Int.MAX_VALUE
            }
        }
    }

    private val sortedOperations: MutableMap<OperationType, List<Any>> = mutableMapOf<OperationType, List<Any>>().apply {
        putAll(OperationType.entries.associateWith { type ->
            this@MultiCatchingFunctions.allDelegates.sortedBy { delegate ->
                comparator(delegate, type)
            }
        })
    }

    inner class Operation<IN, OUT>(val type: OperationType,
                                   val precedence: OperationPrecedence,
                                   val condition: ((ELEM1, ELEM2, IN) -> Result<Boolean>)? = null,
                                   val tryFn1: (ELEM1, IN) -> Any?,
                                   val tryFn2: (ELEM2, IN) -> Any?,
                                   val onFailure1: OnFailure<ELEM1, IN> = OnFailure(),
                                   val onFailure2: OnFailure<ELEM2, IN> = OnFailure(),
                                   val finallyFn1: ((ELEM1, IN) -> Any?)? = null,
                                   val finallyFn2: ((ELEM2, IN) -> Any?)? = null,
                                   val combineFn: ((List<Pair<Any, OUT?>>) -> OUT)? = null,
                                   val initialStateFn: (() -> IN?)? = null) {

        private var initialState: IN? = null

        private fun retryOnFailure(any: Any, param: IN, result: Result<*>): Result<*> {
            return if (any.isDelegate1) {
                val elem : ELEM1 = any as ELEM1
                if (result.isFailure) {
                    onFailure1.retry(elem, param, tryFn1, result.exceptionOrNull())
                } else {
                    result
                }
            } else if (any.isDelegate2) {
                val elem: ELEM2 = any as ELEM2
                if (result.isFailure) {
                    onFailure2.retry(elem, param, tryFn2, result.exceptionOrNull())
                } else {
                    result
                }
            } else {
                result
            }
        }

        private fun undoOnFailure(any: Any, param: IN): Result<*> {
            return if (any.isDelegate1) {
                val e1 = any as ELEM1
                onFailure1.undo(e1, param, initialState)
            } else if (any.isDelegate2) {
                val e2 = any as ELEM2
                onFailure2.undo(e2, param, initialState)
            } else {
                Result.success(null)
            }
        }

        private fun tryFn(any: Any?, param: IN): Any? {
            return if (any.isDelegate1) {
                val e1 = any as ELEM1
                tryFn1.invoke(e1, param)
            } else if (any.isDelegate2) {
                val e2 = any as ELEM2
                tryFn2.invoke(e2, param)
            } else {
                throw IllegalStateException("Unrecognized class: ${any?.javaClass}")
            }
        }

        private fun finallyFn(any: Any?, param: IN): Any? {
            return if (any.isDelegate1) {
                val e1 = any as ELEM1
                finallyFn1?.invoke(e1, param)
            } else if (any.isDelegate2) {
                val e2 = any as ELEM2
                finallyFn2?.invoke(e2, param)
            } else {
                throw IllegalStateException("Unrecognized class: ${any?.javaClass}")
            }
        }

        operator fun invoke(param: IN, debug: Boolean = false): OUT? {
            // cond
            if (condition != null) {
                val conditionResult = delegates1.all { d1 ->
                    delegates2.all { d2 ->
                        val r = condition(d1, d2, param)
                        val throwable = r.exceptionOrNull()
                        if (r.isFailure && throwable != null) {
                            logger.error("Condition failed! Not invoking.", throwable)
                            return@all false
                        }
                        r.getOrElse { false }
                    }
                }
                if (!conditionResult) {
                    logger.error("Condition failed! Not invoking.")
                    return null
                }
            }

            // initial state
            if(onFailure1.requiresInitialState || onFailure2.requiresInitialState) {
                this.initialState = runCatching { initialStateFn?.invoke() }.getOrNull()
            }

            val operationDelegates = sortedOperations[type] ?: return null
            val delegatePairs = mutableListOf<Pair<Any, Result<Any?>>>()

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
                if (values.size == 1 && combineFn == null) {
                    values.firstOrNull() as? OUT
                } else {
                    combineFn?.invoke(values)
                }
            } else {
                null
            }

            // catch
            var exceptions = errors.mapNotNullTo(mutableSetOf()) { (_, result) -> result.exceptionOrNull() }
            if (errors.isNotEmpty() && (onFailure1.hasUndo || onFailure2.hasUndo)) {
                if (debug) {
                    val multiError = MultiException(errors = exceptions.toTypedArray())
                    logger.error(multiError.message, multiError)
                }
                exceptions = mutableSetOf()
                val (success1, success2) = success.partition { (delegate, _) ->
                    delegate.isDelegate1
                }
                val (errors1, errors2) = errors.partition { (delegate, _) ->
                    delegate.isDelegate1
                }
                val delegatePairs1 = success1.union(errors1)
                val delegatePairs2 = success2.union(errors2)

                val undoSubjects1 = when(onFailure1.undoStrategy) {
                    UndoStrategy.SUCCESS -> success1
                    UndoStrategy.FAILURE -> errors1
                    else -> delegatePairs1
                }

                val undoSubjects2 = when(onFailure2.undoStrategy) {
                    UndoStrategy.SUCCESS -> success2
                    UndoStrategy.FAILURE -> errors2
                    else -> delegatePairs2
                }

                undoSubjects1.forEach { (delegate, _) ->
                    undoOnFailure(delegate, param).onFailure { exception ->
                        exceptions.add(exception)
                        if (debug) {
                            logger.error("There was an error undoing the operation!", exception)
                        }
                    }
                }

                undoSubjects2.forEach { (delegate, _) ->
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
                    finallyFn(delegate, param)
                }.onFailure { exception ->
                    exceptions.add(exception)
                    if (debug) {
                        logger.error("Exception in finally block!", exception)
                    }
                }
            }

            if (exceptions.isNotEmpty()) {
                throw MultiException(errors = exceptions.toTypedArray())
            }

            return ret
        }

    }

}
