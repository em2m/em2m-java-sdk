package io.em2m.simplex.std

import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicSimplexModule
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.PathKeyHandler

class StandardModule : BasicSimplexModule() {

    override fun configure(simplex: Simplex) {
        keys(Numbers.keys)
        keys(Dates.keys)
        keys(Bools.keys)
        key(Key("repeat", "*"), PathKeyHandler(simplex, "repeat"))
        key(Key("var", "*"), PathKeyHandler(simplex, "variables"))

        transforms(Numbers.pipes)
        transforms(Strings.pipes)
        transforms(I18n.pipes)
        transforms(Dates.pipes)
        transforms(Arrays.pipes)
        transforms(Bytes.pipes)
        transforms(Bools.pipes(simplex))
        transforms(Objects.pipes)

        conditions(Strings.conditions)
        conditions(Numbers.conditions)
        conditions(Bools.conditions)
        conditions(Dates.conditions)
    }
}