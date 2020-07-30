package io.em2m.actions.servlet

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class AbstractCorsServlet : HttpServlet() {

    var maxAge = 600.toString()
    var allowHeaders = listOf("Authorization, Origin, X-Requested-With, Content-Type, Accept").joinToString(",")
    var allowMethods = listOf("POST, GET, OPTIONS, PUT, DELETE").joinToString(",")
    private val allowCredentials = true
    private val allowOrigin = "*"

    override fun doOptions(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.addHeader("Access-Control-Max-Age", maxAge)
        resp.addHeader("Access-Control-Allow-Headers", allowHeaders)
        resp.addHeader("Access-Control-Allow-Methods", allowMethods)

        val origin = req.getHeader("Origin")
        if (origin != null) {
            resp.addHeader("Access-Control-Allow-Origin", origin)
            resp.addHeader("Access-Control-Allow-Credentials", "$allowCredentials")
        } else {
            resp.addHeader("Access-Control-Allow-Origin", allowOrigin)
        }
    }
}
