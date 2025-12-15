package io.em2m.transactions

inline fun <reified DELEGATE, INPUT: Any, OUTPUT> DELEGATE.transact(onFailure: ((context: TransactionContext<*,*,*>) -> Unit)? = null, crossinline main: ((input: INPUT) -> OUTPUT?)): TransactionHandler {
    val clazz = (this as Any).javaClass as Class<DELEGATE>
    val transaction = Transaction.Builder<DELEGATE, INPUT, OUTPUT>()
        .main { context -> main(context.input!!) }
        .onFailure { context -> onFailure?.invoke(context) }
        .build()
    return TransactionHandler().transact(clazz, transaction)
}
