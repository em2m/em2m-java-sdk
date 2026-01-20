package io.em2m.transactions

import kotlin.test.Test

class TransactionTest {

    @Test
    fun `input method`() {
        val modifiable = mutableListOf<String>()
        val transaction = Transaction.Builder<Any, MutableList<String>, Any?>()
            .main { _, context ->
                context.input?.add("hello")
                null
            }
            .build()
        val context = transaction.toContext(listOf(this))
        val handler = TransactionHandler()
        handler.transact(this, transaction)

        handler(context, modifiable)
        assert("hello" in modifiable)
    }

    @Test
    fun `delegate method`() {
        val list = mutableListOf<Int>()
        val transaction = Transaction.Builder<MutableList<Int>, Int, Any?>()
            .main { delegate, context ->
                delegate.add(context.input!!)
            }
            .build()
        val context = transaction.toContext(listOf(list))
        val handler = TransactionHandler()
        handler(context, 5)
        assert(5 in list)
    }

    @Test
    fun `initial value set`() {
        val transaction = Transaction.Builder<MutableList<String>, String, Any?>()
            .main { delegate, context ->
                delegate.add(context.input!!)
            }.build()

        val delegate = mutableListOf("hello")
        val context = transaction.toContext(listOf(delegate))
        val handler = TransactionHandler()
        handler(context, "world")

        assert("hello" in delegate)
        assert("world" in delegate)
    }

}
