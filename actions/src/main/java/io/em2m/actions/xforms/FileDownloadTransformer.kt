package io.em2m.actions.xforms


import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Priorities

class FileDownloadTransformer(override val priority: Int = Priorities.PRE_PARSE) : ActionTransformer {

    @SuppressWarnings()
    override fun doOnNext(ctx: ActionContext) {
        if ("GET" == ctx.environment["Method"]) {
            val params = ctx.environment["Parameters"] as Map<String, Array<String>>
            val accept = params["accept"]?.firstOrNull()
            val filename = params["filename"]?.firstOrNull()
            val body = params["body"]?.firstOrNull()
            val contentType = params["contentType"]?.firstOrNull()

            if (accept != null) {
                (ctx.environment["Headers"] as (MutableMap<String, Any?>))["accept"] = listOf(accept)
            }
            if (body != null) {
                ctx.inputStream = body.byteInputStream()
            }
            if (contentType != null) {
                ctx.environment["ContentType"] = contentType
            }
            if (filename != null) {
                ctx.response.headers.set("Content-Disposition", "attachment;filename=$filename")
            }
        }
    }

}
