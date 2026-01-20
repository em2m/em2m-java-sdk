package io.em2m.transactions

class TransactionException(override val message: String? = null, val context: TransactionContext<*,*,*>, vararg val errors: Throwable): Exception() {

    constructor(message: String, context: TransactionContext<*, *, *>, other: TransactionException) : this(message, context, errors= other.errors)

    private val _stacktrace : List<StackTraceElement> = errors.flatMap{ it.stackTrace.toList() }
    private val stacktrace : Array<out StackTraceElement?> = Array(_stacktrace.size) { _stacktrace[it] }

    override fun getStackTrace(): Array<out StackTraceElement?> {
        return this.stacktrace
    }

}
