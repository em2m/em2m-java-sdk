package io.em2m.transactions

interface Undoable<DELEGATE, INPUT, OUTPUT> : UndoOnFailureAction<DELEGATE, INPUT> {

    override fun invoke(elem: DELEGATE, param: INPUT, initial: INPUT): Any? {
        return undo(elem, param, initial)
    }

    fun undo(elem: DELEGATE, newValue: INPUT, oldValue: INPUT): OUTPUT

    fun finally(elem: DELEGATE, newValue: INPUT) = {}

    fun initialState(): INPUT? = null

}
