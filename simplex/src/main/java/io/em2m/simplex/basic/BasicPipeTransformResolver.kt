package io.em2m.simplex.basic

import io.em2m.simplex.model.PipeTransform
import io.em2m.simplex.model.PipeTransformResolver


class BasicPipeTransformResolver(val handlers: Map<String, PipeTransform>) : PipeTransformResolver {

    override fun find(key: String): PipeTransform? {
        return handlers[key]
    }
}