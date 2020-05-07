package io.em2m.problem

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feign.FeignException.errorStatus
import feign.Response
import feign.codec.ErrorDecoder

class ProblemErrorDecoder : ErrorDecoder {

    private val mapper = jacksonObjectMapper()

    override fun decode(methodKey: String?, response: Response): Exception {
        return if (response.headers()["Content-Type"]?.firstOrNull() == "application/json") {
            val problem: Problem = mapper.readValue(response.body().asInputStream())
            return ProblemException(problem)
        } else errorStatus(methodKey, response)
    }
}