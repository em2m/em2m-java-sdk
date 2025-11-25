package io.em2m.obj

interface Combinable<DELEGATE, VALUE> {

    fun combine(results: List<Pair<DELEGATE, VALUE?>>): VALUE

}
