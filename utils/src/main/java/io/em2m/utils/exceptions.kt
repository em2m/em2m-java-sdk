package io.em2m.utils


class MultiException(override val message: String? = null, vararg val errors: Throwable): Exception() {

    constructor(message: String, other: MultiException) : this(message, errors= other.errors)

    private val _stacktrace : List<StackTraceElement> = errors.flatMap{ it.stackTrace.toList() }
    private val stacktrace : Array<out StackTraceElement?> = Array(_stacktrace.size) { _stacktrace[it] }

    override fun getStackTrace(): Array<out StackTraceElement?> {
        return this.stacktrace
    }

}
