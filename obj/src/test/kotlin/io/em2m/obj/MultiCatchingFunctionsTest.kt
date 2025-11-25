package io.em2m.obj

import io.em2m.utils.MultiException
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiCatchingFunctionsTest {

    @Test
    fun default() {
        data class Delegate1(var modifiable: String)
        data class Delegate2(var modifiable: String)

        val d1 = Delegate1("d1.initial")
        val d2 = Delegate2("d2.initial")

        val mcfs = MultiCatchingFunctions<Delegate1, Delegate2>(delegates1 = listOf(d1), delegates2 = listOf(d2))
        val op = mcfs.Operation<Unit, String>(OperationType.READ, OperationPrecedence.ANY,
            tryFn1 = {delegate1, _ ->
                delegate1.modifiable
            },
            tryFn2 = {delegate2, _ ->
                delegate2.modifiable
            }
        )
        val result = op(Unit)
        assertEquals("d1.initial", result)
    }

    @Test
    fun `null`() {
        data class Delegate1(var modifiable: String?)
        data class Delegate2(var modifiable: String?)

        val d1 = Delegate1("combined.initial")
        val d2 = Delegate2("combined.initial")

        val mcfs = MultiCatchingFunctions<Delegate1, Delegate2>(delegates1 = listOf(d1), delegates2 = listOf(d2))
        val op = mcfs.Operation<String?, String>(OperationType.READ, OperationPrecedence.ALL,
            tryFn1 = { _, input ->
                input!!
            },
            tryFn2 = { _, input ->
                input!!
            })
        assertThrows<MultiException> { op(null) }
    }

    @Test
    fun undo() {
        data class Delegate1(var modifiable: String?)
        data class Delegate2(var modifiable: String?)

        val d1 = Delegate1("combined.initial")
        val d2 = Delegate2("combined.initial")

        val mcfs = MultiCatchingFunctions<Delegate1, Delegate2>(delegates1 = listOf(d1), delegates2 = listOf(d2))
        val op = mcfs.Operation<String?, String>(OperationType.UPDATE, OperationPrecedence.ANY,
            tryFn1 = {delegate1, input ->
                delegate1.modifiable = input
                input!! // throws NPE
            },
            tryFn2 = {delegate2, input ->
                delegate2.modifiable = input
                input!! // throws NPE
            },
            onFailure1 = OnFailure(undoAction = {delegate1, param, initial ->
                delegate1.modifiable = initial
            }),
            onFailure2 = OnFailure(undoAction = {delegate2, param, initial ->
                delegate2.modifiable = initial
            }),
            initialStateFn = { "combined.initial" }
        )
        val result = op(null)

        assertEquals("combined.initial", d1.modifiable)
        assertEquals("combined.initial", d2.modifiable)
    }

}
