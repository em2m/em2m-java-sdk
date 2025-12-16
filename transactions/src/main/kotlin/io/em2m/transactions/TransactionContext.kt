package io.em2m.transactions

data class TransactionContext<DELEGATE, INPUT : Any, OUTPUT>(
    val clazz: Class<*> = Any::class.java,
    val delegates: List<DELEGATE>,
    var errors: MutableSet<Throwable> = mutableSetOf(),
    var inputClass: Class<INPUT>? = null,
    var outputClass: Class<OUTPUT>? = null,
    var allowNullInput: Boolean = false,
    var allowNullOutput: Boolean = false,
    var transaction: Transaction<DELEGATE, INPUT, OUTPUT>,
    val scope: Map<String, Any?> = mutableMapOf(),
    val debug: Boolean = false
) {

    var condition: Boolean? = null
    var initial: INPUT? = null

    operator fun invoke(input: INPUT): TransactionContext<DELEGATE, INPUT, OUTPUT> = apply {
        this.input = input
    }

    var input: INPUT? = null
        set(value) {
            if (value == null && allowNullInput || value != null) {
                field = value
            } else {
                errors.add(NullPointerException("Can't set a non-nullable variable, set allowNullInput to true"))
            }
        }

    val validInput: Boolean
        get() {
            return input == null && allowNullInput || input != null
        }

    var output: OUTPUT? = null
        internal set(value) {
            if (value == null && allowNullOutput || value != null) {
                field = value
            } else {
                errors.add(NullPointerException("Can't set a non-nullable variable, set allowNullOutput to true"))
            }
        }

    val validOutput: Boolean
        get() {
            return output == null && allowNullOutput || output != null
        }

    val success: Boolean
        get() = errors.isEmpty() && validInput && validOutput


    fun <T> safeTry(block: () -> T) {
        try {
            block()
        } catch (ex : Exception) {
            errors.add(ex)
        }
    }

    fun <T> tryOr(default: T, block: () -> T): T {
        return try {
            block()
        } catch (ex : Exception) {
            errors.add(ex)
            default
        }
    }

    fun <T> tryOrNull(block: () -> T): T? {
        return try {
            block()
        } catch (ex : Exception) {
            errors.add(ex)
            null
        }
    }

    fun <T> tryOrThrow(msg: String? = null, block: () -> T): T {
        return try {
            block()
        } catch (ex : Exception) {
            val throwable = TransactionException(msg, context=this, ex)
            errors.add(throwable)
            throw throwable
        }
    }

}
