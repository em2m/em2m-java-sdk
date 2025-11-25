package io.em2m.obj

interface Transaction<DELEGATE, INPUT, OUTPUT> {

    fun condition(elem: DELEGATE, input: INPUT): Boolean = true

    fun run(elem: DELEGATE, input: INPUT): OUTPUT

}
