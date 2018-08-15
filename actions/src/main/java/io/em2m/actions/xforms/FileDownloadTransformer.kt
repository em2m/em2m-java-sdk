package io.em2m.actions.xforms


import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.flows.Priorities
import rx.Observable

class FileDownloadTransformer(override val priority: Int = Priorities.PRE_PARSE) : ActionTransformer {

    override fun call(obs: Observable<ActionContext>): Observable<ActionContext> {

        return obs.doOnNext { context ->
            if ("GET" == context.environment["Method"]) {
                val params = context.environment["Parameters"] as Map<String, Array<String>>
                val accept = params["accept"]?.firstOrNull()
                val filename = params["filename"]?.firstOrNull()
                val body = params["body"]?.firstOrNull()
                val contentType = params["contentType"]?.firstOrNull()

                if (accept != null) {
                    (context.environment["Headers"] as (MutableMap<String, Any?>))["accept"] = listOf(accept)
                }
                if (body != null) {
                    context.inputStream = body.byteInputStream()
                }
                if (contentType != null) {
                    context.environment["ContentType"] = contentType
                }
                if (filename != null) {
                    context.response.headers.set("Content-Disposition", "attachment;filename=$filename")
                }
            }
        }
    }

}
