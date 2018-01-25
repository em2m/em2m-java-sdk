package io.em2m.actions.runtimes

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class AbstractCorsServlet : HttpServlet() {

    var maxAge: Int = 600
    var allowHeaders = listOf("Authorization, Origin, X-Requested-With, Content-Type, Accept")
    var allowMethods = listOf("POST, GET, OPTIONS, PUT, DELETE")
    val allowCredentials = true
    val allowOrigin = "*"

    override fun doOptions(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.addHeader("Access-Control-Max-Age", "$maxAge")
        resp.addHeader("Access-Control-Allow-Headers", allowHeaders.joinToString(","))
        resp.addHeader("Access-Control-Allow-Methods", allowMethods.joinToString(","))

        val origin = req.getHeader("Origin")
        if (origin != null) {
            resp.addHeader("Access-Control-Allow-Origin", origin)
            resp.addHeader("Access-Control-Allow-Credentials", "$allowCredentials")
        } else {
            resp.addHeader("Access-Control-Allow-Origin", allowOrigin)
        }
    }
}
