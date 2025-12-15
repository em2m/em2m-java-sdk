package io.em2m.transactions

import io.em2m.utils.MultiException
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiCatchingFunctionTest {

    @Test
    fun default() {
        data class Input(val value: String? = null)

        val mcf = MultiFunction<Any>(this)
        val operation = mcf.Operation<Input, String>(TransactionType.READ,
            TransactionPrecedence.ALL, tryFn = { _, param: Input ->
                param.value!!
            })

        val expected = "hello"

        assertEquals(operation(Input(expected)), expected)
    }

    @Test
    fun `null`() {
        data class Input(val value: String? = null)

        val mcf = MultiFunction<Any>(this)
        val operation = mcf.Operation<Input, String>(TransactionType.READ,
            TransactionPrecedence.ALL, tryFn = { _, param: Input ->
                param.value!!
            })

        assertThrows<MultiException> { operation(Input(null)) }
    }

    @Test
    fun undo() {
        data class Input(val value: String? = null)

        data class Delegate(var modifiable: Input)

        val delegate = Delegate(Input("hello world"))

        val mcf = MultiFunction(delegate)
        val operation = mcf.Operation(TransactionType.UPDATE,
            TransactionPrecedence.ALL, tryFn = { elem, param: Input ->
                elem.modifiable = param
                null!! // throws npe
                elem.modifiable.value
            }, onFailure = OnFailure(undoAction = { elem, _, initial ->
                elem.modifiable = initial
            }), initialStateFn = { delegate.modifiable })
        operation(Input("goodbye stranger"))
        assertEquals(delegate.modifiable.value, "hello world")
    }

}
