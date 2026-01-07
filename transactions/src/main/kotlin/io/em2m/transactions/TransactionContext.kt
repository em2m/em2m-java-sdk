package io.em2m.transactions

data class TransactionContext<DELEGATE, INPUT : Any, OUTPUT>(
    val clazz: Class<*> = Any::class.java,
    val delegates: List<DELEGATE>,
    var errors: MutableSet<Throwable> = mutableSetOf(),
    var inputClass: Class<INPUT>? = null,
    var outputClass: Class<OUTPUT>? = null,
    var transaction: Transaction<DELEGATE, INPUT, OUTPUT>,
    val scope: Map<String, Any?> = mutableMapOf(),
    var config: TransactionConfig = TransactionConfig.DEFAULT,
    val debug: Boolean = false
) {

    var condition: Boolean? = null
    var initial: INPUT? = null

    operator fun invoke(input: INPUT): TransactionContext<DELEGATE, INPUT, OUTPUT> = apply {
        this.input = input
    }

    var input: INPUT? = null

    var output: OUTPUT? = null

    val success: Boolean
        get() = errors.isEmpty()


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
