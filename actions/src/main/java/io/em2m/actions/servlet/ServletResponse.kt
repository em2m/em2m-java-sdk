package io.em2m.actions.servlet

import io.em2m.actions.model.Response
import java.io.OutputStream
import javax.servlet.http.HttpServletResponse


class ServletResponse(
        private val servletResponse: HttpServletResponse,
        override var entity: Any? = null) : Response {

    override var contentType: String?
        get() = headers.get("Content-Type")
        set(value) = headers.set("Content-Type", value)

    override val headers: Response.Headers = ServletResponseHeaders()

    override var statusCode: Int
        get() = servletResponse.status
        set(value) {
            servletResponse.status = value
        }

    override val outputStream: OutputStream = servletResponse.outputStream

    inner class ServletResponseHeaders() : Response.Headers {

        override fun set(key: String, value: String?) {
            servletResponse.setHeader(key, value)
        }

        override fun get(key: String): String? {
            return servletResponse.getHeader(key)
        }
    }
}