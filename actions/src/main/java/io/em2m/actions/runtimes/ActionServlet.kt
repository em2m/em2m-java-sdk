package io.em2m.actions.runtimes

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


abstract class ActionServlet() : HttpServlet() {

    abstract val runtime: ServletRuntime

    open fun actionName(req: HttpServletRequest): String {
        // assume a pattern /actions/{actionName} mapped to /action/*
        return req.pathInfo.substring(1)
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        runtime.process(actionName(req), req, resp)
    }

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val actionName = req.pathInfo.substring(1)
        runtime.process(actionName, req, resp)
    }

}