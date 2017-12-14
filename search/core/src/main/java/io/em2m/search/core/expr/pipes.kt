package io.em2m.search.core.expr

import io.em2m.simplex.model.PipeTransform


class UpperCasePipe : PipeTransform {
    override fun transform(value: Any?): Any? {
        return if (value != null) {
            value.toString().toUpperCase()
        } else null
    }

}

class CapitalizePipe : PipeTransform {
    override fun transform(value: Any?): Any? {
        return if (value != null) {
            value.toString().capitalize()
        } else null
    }

}
