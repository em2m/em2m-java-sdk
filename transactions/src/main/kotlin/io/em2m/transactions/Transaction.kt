package io.em2m.transactions

private class DelegateTransaction<DELEGATE, INPUT: Any, OUTPUT>(
    private val runFn: (delegate:DELEGATE, context: TransactionContext<DELEGATE, INPUT, OUTPUT>) -> OUTPUT?,
    private val conditionFn: ((delegate: DELEGATE, context: TransactionContext<DELEGATE, INPUT, OUTPUT>) -> Boolean) = { _, _ -> true },
    private val initialValueFn: ((TransactionContext<DELEGATE, INPUT, OUTPUT>) -> INPUT)? = null,
    private val onCreateFn: ((TransactionContext<*, *, *>) -> Unit)? = null,
    private val onFailureFn: ((TransactionContext<*, *, *>) -> Unit)? = null,
    private val onSuccessFn: ((TransactionContext<*, *, *>) -> Unit)? = null,
    private val onCompleteFn: ((TransactionContext<*, *, *>) -> Unit)? = null,
    private val onStateChaneFn: ((TransactionContext<*, *, *>) -> Unit)? = null,
    private val combineFn: ((List<OUTPUT>) -> OUTPUT?)? = null,
    override var state: TransactionState = TransactionState.CREATED,
    override var type: TransactionType = TransactionType.READ,
    override var precedence: TransactionPrecedence = TransactionPrecedence.ALL
) : Transaction<DELEGATE, INPUT, OUTPUT>() {

    override fun run(delegate:DELEGATE, context: TransactionContext<DELEGATE, INPUT, OUTPUT>): OUTPUT? {
        return runFn(delegate, context)
    }

    override fun condition(delegate: DELEGATE, context: TransactionContext<DELEGATE, INPUT, OUTPUT>): Boolean {
        return runCatching { conditionFn(delegate, context) }.getOrDefault(true)
    }

    override fun initialValue(context: TransactionContext<DELEGATE, INPUT, OUTPUT>): INPUT? {
        return initialValueFn?.invoke(context)
    }

    override fun onCreate(context: TransactionContext<*, *, *>) {
        onCreateFn?.invoke(context)
    }

    override fun onFailure(context: TransactionContext<*, *, *>) {
        onFailureFn?.invoke(context)
    }

    override fun onSuccess(context: TransactionContext<*, *, *>) {
        onSuccessFn?.invoke(context)
    }

    override fun onComplete(context: TransactionContext<*, *, *>) {
        onCompleteFn?.invoke(context)
    }

    override fun onStateChange(context: TransactionContext<*, *, *>) {
        super.onStateChange(context)
        this.onStateChaneFn?.invoke(context)
    }

    override fun combine(results: List<OUTPUT>): OUTPUT? {
        return combineFn?.invoke(results)
    }

}

abstract class Transaction<DELEGATE, INPUT : Any, OUTPUT> : AbstractOnStateChangeListener() {

    open var state:         TransactionState        = TransactionState.CREATED
    open var type:          TransactionType         = TransactionType.READ
    open var precedence:    TransactionPrecedence   = TransactionPrecedence.ALL

    open fun condition(delegate: DELEGATE, context: TransactionContext<DELEGATE, INPUT, OUTPUT>): Boolean = true

    open fun onCreate(context: TransactionContext<*, *, *>) {}

    open fun combine(results: List<OUTPUT>): OUTPUT? {
        return results.firstOrNull()
    }

    abstract fun onFailure(context: TransactionContext<*, *, *>)

    open fun onSuccess(context: TransactionContext<*, *, *>) {}

    open fun onComplete(context: TransactionContext<*, *, *>) {}

    override fun onStateChange(context: TransactionContext<*, *, *>) {
        when(context.transaction.state) {
            TransactionState.CREATED -> this.onCreate(context)
            TransactionState.FAILURE -> this.onFailure(context)
            TransactionState.SUCCESS -> this.onSuccess(context)
            TransactionState.COMPLETED -> this.onComplete(context)
            else -> {}
        }
    }

    open fun initialValue(context: TransactionContext<DELEGATE, INPUT, OUTPUT>): INPUT? = null

    abstract fun run(delegate: DELEGATE, context: TransactionContext<DELEGATE, INPUT, OUTPUT>): OUTPUT?

    class Builder<DELEGATE, INPUT : Any, OUTPUT> {

        private var clazz: Class<*> = Any::class.java
        fun delegateClass(clazz: Class<*>) = apply {
            this.clazz = clazz
        }

        private lateinit var runFn: ((delegate:DELEGATE, context: TransactionContext<DELEGATE, INPUT, OUTPUT>) -> OUTPUT?)
        fun main(fn: (delegate:DELEGATE, context: TransactionContext<DELEGATE, INPUT, OUTPUT>) -> OUTPUT?) = apply {
            this.runFn = fn
        }

        private var conditionFn: ((delegate: DELEGATE, context: TransactionContext<DELEGATE, INPUT, OUTPUT>) -> Boolean) = { _, _ -> true }
        fun condition(fn: ((DELEGATE, TransactionContext<DELEGATE, INPUT, OUTPUT>) -> Boolean)) = apply {
            this.conditionFn = fn
        }

        private var initialValueFn: ((TransactionContext<DELEGATE, INPUT, OUTPUT>) -> INPUT)? = null
        fun initialValue(fn: ((TransactionContext<DELEGATE, INPUT, OUTPUT>) -> INPUT)?) = apply {
            this.initialValueFn = fn
        }

        private var onCreateFn: ((TransactionContext<*, *, *>) -> Unit)? = null
        fun onCreate(fn: (TransactionContext<*, *, *>) -> Unit) = apply  {
            this.onCreateFn = fn
        }

        private var onFailureFn: ((TransactionContext<*, *, *>) -> Unit)? = null
        fun onFailure(fn: (TransactionContext<*, *, *>) -> Unit) = apply  {
            this.onFailureFn = fn
        }

        private var onSuccessFn: ((TransactionContext<*, *, *>) -> Unit)? = null
        fun onSuccess(fn: (TransactionContext<*, *, *>) -> Unit) = apply  {
            this.onSuccessFn = fn
        }

        private var onCompleteFn: ((TransactionContext<*, *, *>) -> Unit)? = null
        fun onComplete(fn: (TransactionContext<*, *, *>) -> Unit) = apply  {
            this.onCompleteFn = fn
        }

        private var onStateChaneFn: ((TransactionContext<*, *, *>) -> Unit)? = null
        fun onStateChange(fn: (TransactionContext<*, *, *>) -> Unit) = apply  {
            this.onStateChaneFn = fn
        }

        private var combineFn: ((List<OUTPUT>) -> OUTPUT?)? = null
        fun combine(fn: (List<OUTPUT>) -> OUTPUT?) = apply {
            this.combineFn = fn
        }

        private var state: TransactionState = TransactionState.CREATED

        private var type: TransactionType = TransactionType.READ
        fun type(type: TransactionType) = apply { this.type = type}

        private var precedence: TransactionPrecedence = TransactionPrecedence.ALL
        fun precedence(precedence: TransactionPrecedence) = apply { this.precedence = precedence }

        fun build(): Transaction<DELEGATE, INPUT, OUTPUT> {
            if (!this::runFn.isInitialized) {
                throw IllegalStateException("Main function wasn't initialized!")
            }
            return DelegateTransaction(
                runFn = this.runFn,
                conditionFn = this.conditionFn,
                initialValueFn = this.initialValueFn,
                onCreateFn = this.onCreateFn,
                onFailureFn = this.onFailureFn,
                onSuccessFn = this.onSuccessFn,
                onCompleteFn = this.onCompleteFn,
                onStateChaneFn = this.onStateChaneFn,
                combineFn = this.combineFn,
                type = this.type,
                precedence = this.precedence,
                state = this.state
            )
        }

    }

}
