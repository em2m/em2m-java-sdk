package io.em2m.simplex.std

import io.em2m.simplex.model.BasicPipeTransformResolver
import io.em2m.simplex.model.ExprContext
import io.em2m.simplex.model.PipeTransform


class SelectPipe : PipeTransform {

    var mappingName: String = "mapping"

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            mappingName = args[0]
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val mapping = context[mappingName] as? Map<*, *>
                ?: throw IllegalArgumentException("No value found for pipe parameter")
        return mapping[value.toString()] ?: mapping["other"] ?: value
    }

}

object I18n {

    val pipes = BasicPipeTransformResolver(mapOf("select" to SelectPipe()))

}