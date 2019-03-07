package io.em2m.actions.xforms


import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Problem
import io.em2m.flows.Priorities
import io.em2m.policy.model.Claims
import rx.Observable
import java.util.*

class JwtTransformer(val secretKey: String, val requireAuth: Boolean = false, override val priority: Int = Priorities.AUTHENTICATE) : ActionTransformer {

    val mapper = jacksonObjectMapper()
    val decoder = Base64.getDecoder()

    override fun call(obs: Observable<ActionContext>): Observable<ActionContext> {

        return obs.doOnNext { context ->
            val token = context.environment["Token"] as? String
            if (token != null && token.isNotEmpty()) {
                context.claims = parseToken(token)
            } else if (requireAuth) {
                Problem(status = Problem.Status.NOT_AUTHORIZED, title = "Not Authorized").throwException()
            }
        }
    }

    private fun parseToken(token: String): Claims {
        return try {
            val algorithm = Algorithm.HMAC256(secretKey)
            val jwtVerifier = JWT.require(algorithm).build()
            val jwt = jwtVerifier.verify(token)
            val values: Map<String, Any?> = mapper.readValue(decoder.decode(jwt.payload))
            return Claims(values)
        } catch (ex: Exception) {
            Problem(status = Problem.Status.NOT_AUTHORIZED, title = "Not Authorized", detail = ex.message).throwException()
        }
    }

}
