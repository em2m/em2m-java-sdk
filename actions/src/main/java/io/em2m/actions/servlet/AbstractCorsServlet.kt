package io.em2m.actions.servlet

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class AbstractCorsServlet : HttpServlet() {

    var maxAge = 600.toString()
    val allowHeaders = listOf(
        "Access-Control-Allow-Headers",
        "Access-Control-Allow-Headers",
        "Access-Control-Allow-Origin",
        "Authorization",
        "Origin",
        "Connection",
        "X-Requested-With",
        "X-Accel-Buffering",
        "Content-Type",
        "Accept",
        "X-Em2m-Timezone"
    ).joinToString(",")
    var allowMethods = listOf("POST, GET, OPTIONS, PUT, DELETE").joinToString(",")
    private val allowCredentials = true
    private val allowOrigin = "*"

    override fun doOptions(req: HttpServletRequest, resp: HttpServletResponse) {
        addCorsHeaders(resp)
    }

    fun addCorsHeaders(resp: HttpServletResponse) {
        resp.addHeader("Access-Control-Max-Age", maxAge)
        resp.addHeader("Access-Control-Allow-Headers", allowHeaders)
        resp.addHeader("Access-Control-Allow-Methods", allowMethods)
        resp.addHeader("Access-Control-Allow-Origin", allowOrigin)
    }
}
