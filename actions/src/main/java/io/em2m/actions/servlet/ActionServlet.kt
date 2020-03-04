package io.em2m.actions.servlet

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class ActionServlet() : HttpServlet() {

    abstract val runtime: ServletRuntime

    open fun actionName(req: HttpServletRequest): String {
        // assume a pattern /actions/{actionName} mapped to /action/*
        return req.pathInfo.substring(1).removeSuffix("/")
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        runtime.process(actionName(req), req, resp)
    }

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        runtime.process(actionName(req), req, resp)
    }

}