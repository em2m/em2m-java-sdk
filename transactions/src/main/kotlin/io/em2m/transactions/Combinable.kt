package io.em2m.transactions

interface Combinable<DELEGATE, VALUE> {

    fun combine(results: List<Pair<DELEGATE, VALUE?>>): VALUE

}
