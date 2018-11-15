package io.em2m.actions.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.actions.model.Problem
import io.em2m.actions.model.Problem.Companion.notFound
import java.io.InputStream
import java.io.OutputStream


abstract class ActionRequestStreamHandler : RequestStreamHandler {

    abstract val mapper: ObjectMapper
    abstract val runtime: LambdaRuntime

    open fun actionName(req: LambdaRequest): String {
        // assume a pattern /actions/{actionName} mapped to /action/*
        val requestUri = req.path ?: req.requestUri ?: notFound({ "Missing action" })
        return requestUri.substringAfterLast("/")
    }

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        val req = mapper.readValue(input, LambdaRequest::class.java)
        val res = handlRequest(req, context)
        mapper.writeValue(output, res)
    }

    open fun handlRequest(request: LambdaRequest, context: Context): LambdaResponse.ResponseData {
        val actionName = actionName(request)
        return runtime.process(actionName, request).toData(mapper)
    }

}