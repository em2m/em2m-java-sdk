package io.em2m.actions.xforms

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Problem
import io.em2m.flows.Priorities
import rx.Observable
import java.io.IOException
import javax.servlet.http.HttpServletResponse

class JacksonResponseTransformer(val objectMapper: ObjectMapper = jacksonObjectMapper(), override val priority: Int = Priorities.RESPONSE) : ActionTransformer {

    override fun call(source: Observable<ActionContext>): Observable<ActionContext> {

        return source.doOnNext { context ->
            try {
                val response = context.response
                if (response.entity != null) {
                    response.contentType = "application/json"
                    response.statusCode = HttpServletResponse.SC_OK
                    objectMapper.writeValue(response.outputStream, response.entity)
                } else {
                    response.statusCode = HttpServletResponse.SC_NO_CONTENT
                }
            } catch (ioEx: IOException) {
                Problem(status = Problem.Status.BAD_REQUEST, title = "Error sending response", detail = ioEx.message).throwException()
            }
        }
    }

}