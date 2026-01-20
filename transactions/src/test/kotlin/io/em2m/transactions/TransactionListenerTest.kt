package io.em2m.transactions

import kotlin.test.Test

class TransactionListenerTest {

    @Test
    fun `on create`() {
        val modifiable = mutableListOf<String>()
        val transaction = Transaction.Builder<Any, String, Any?>()
            .main { _,_ -> null }
            .build()
        val context = transaction.toContext(listOf(this))

        val handler = TransactionHandler()
        handler.onCreate { modifiable.add("derp") }

        handler(context)

        assert("derp" in modifiable)
    }

    @Test
    fun `on fail`() {
        val transaction = Transaction.Builder<Any, Any, Any?>()
            .main { _, _ -> null!! }
            .build()
        val context = transaction.toContext(listOf(this))
        var assertion = false
        val handler = TransactionHandler()
        handler.onFailure { _ ->
            assertion = true
        }
        handler(context)

        assert(assertion)
    }

    @Test
    fun `on success`() {
        val transaction = Transaction.Builder<Any, Any, Any?>()
            .main { _, _ -> null }
            .build()
        val context = transaction.toContext(listOf(this))
        var assertion = false
        val handler = TransactionHandler()
        handler.onSuccess { _ -> assertion = true }
        handler(context)
        assert(assertion)
    }

    @Test
    fun `on good states`(){
        val goodStates = setOf(
            TransactionState.CREATED,
            TransactionState.INITIALIZED,
            TransactionState.RUNNING,
            TransactionState.SUCCESS,
            TransactionState.COMPLETED)

        val visitedStates = mutableSetOf<TransactionState>()
        val transaction = Transaction.Builder<Any, Any, Any?>()
            .main { _, _ -> null }
            .build()

        val context = transaction.toContext(listOf(this))
        val handler = TransactionHandler()
        handler.onStateChange { context ->
            visitedStates.add(context.transaction.state)
        }
        handler(context)
        assert(visitedStates.all { state -> state in goodStates })
    }



}
