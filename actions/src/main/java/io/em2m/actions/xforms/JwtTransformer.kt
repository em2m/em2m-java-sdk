package io.em2m.actions.xforms


import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Priorities
import io.em2m.policy.model.Claims
import io.em2m.problem.Problem
import io.em2m.problem.Problem.Companion.notAuthorized
import java.util.*

class JwtTransformer(val secretKey: String, val requireAuth: Boolean = false, override val priority: Int = Priorities.AUTHENTICATE) : ActionTransformer {

    val mapper = jacksonObjectMapper()
    val decoder = Base64.getDecoder()


    override fun doOnNext(ctx: ActionContext) {
        val token = ctx.environment["Token"] as? String
        if (token != null && token.isNotEmpty()) {
            ctx.claims = parseToken(token)
        } else if (requireAuth) {
            Problem(status = Problem.Status.NOT_AUTHORIZED, title = "Not Authorized").throwException()
        }
    }

    private fun parseToken(token: String): Claims {
        return try {
            val algorithm = Algorithm.HMAC256(secretKey)
            val jwtVerifier = JWT.require(algorithm).build()
            val jwt = jwtVerifier.verify(token)
            val values: Map<String, Any?> = mapper.readValue(decoder.decode(jwt.payload))
            Claims(values)
        } catch (ex: Exception) {
            notAuthorized(title = { "Not Authorized" }, detail = { ex.message })
        }
    }

}
