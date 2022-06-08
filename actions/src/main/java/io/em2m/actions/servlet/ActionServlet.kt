package io.em2m.actions.servlet

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

abstract class ActionServlet() : AbstractCorsServlet() {

    abstract val runtime: ServletRuntime

    open fun actionName(req: HttpServletRequest): String {
        // assume a pattern /actions/{actionName} mapped to /action/*
        return req.pathInfo.substring(1).removeSuffix("/")
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        addCorsHeaders(resp)
        runtime.process(actionName(req), req, resp)
    }

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        addCorsHeaders(resp)
        runtime.process(actionName(req), req, resp)
    }

}
