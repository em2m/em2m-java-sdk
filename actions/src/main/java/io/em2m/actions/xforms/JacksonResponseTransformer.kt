package io.em2m.actions.xforms

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformerSupport
import io.em2m.actions.model.Problem
import io.em2m.flows.Priorities
import rx.Observable
import java.io.IOException
import javax.servlet.http.HttpServletResponse

class JacksonResponseTransformer(val objectMapper: ObjectMapper = jacksonObjectMapper())
    : ActionTransformerSupport(Priorities.RESPONSE) {

    override fun call(source: Observable<ActionContext>): Observable<ActionContext> {

        return source.doOnNext { context ->
            try {
                val response = context.response
                response.contentType = "application/json"
                response.statusCode = HttpServletResponse.SC_OK
                if (response.entity != null) {
                    objectMapper.writeValue(response.outputStream, response.entity)
                }
            } catch (ioEx: IOException) {
                Problem(status = Problem.Status.BAD_REQUEST, title = "Error sending response", detail = ioEx.message).throwException()
            }
        }
    }

}