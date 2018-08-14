package io.em2m.actions.runtimes

import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
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
        val body = req.getParameter("body")
        val contentType = req.getParameter("contentType") ?: "application/json"
        val wrappedRequest = object : HttpServletRequestWrapper(req) {

            override fun getInputStream(): ServletInputStream {

                val stream = body.byteInputStream()

                return object : ServletInputStream() {
                    override fun isReady(): Boolean {
                        return false
                    }

                    override fun isFinished(): Boolean {
                        return false
                    }

                    override fun read(): Int {
                        return stream.read()
                    }

                    override fun setReadListener(p0: ReadListener?) {
                    }

                }
            }

            override fun getContentType(): String {
                return contentType
            }

        }
        runtime.process(actionName, wrappedRequest, resp)
    }

}