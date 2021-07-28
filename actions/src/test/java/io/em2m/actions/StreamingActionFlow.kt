package io.em2m.actions

import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionFlow
import io.em2m.actions.model.ActionTransformer
import java.io.OutputStreamWriter

class StreamingActionFlow : ActionFlow {

    override val transformers: List<ActionTransformer> = emptyList()

    override fun main(context: ActionContext) {
        println("Enter main!")
        context.response.contentType = "text/event-stream; charset=utf-8"
        val out = context.response.outputStream
        val writer = OutputStreamWriter(out)
        (0..1000).forEach {
            writer
                .append("retry: 5000\n")
                .append("event: syncevent\n")
                .append("data: I'm event $it.\n\n");
            writer.flush()
            Thread.sleep(1000)
        }
    }

}
