package io.em2m.actions.xforms


import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.actions.model.Problem
import io.em2m.flows.Priorities
import rx.Observable

class JwtTransformer(val secretKey: String, val requireAuth: Boolean = false, override val priority: Int = Priorities.AUTHENTICATE) : ActionTransformer {

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

    private fun parseToken(token: String): Map<String, Any?> {
        return try {
            val algorithm = Algorithm.HMAC256(secretKey)
            val jwtVerifier = JWT.require(algorithm).build()
            jwtVerifier.verify(token).claims
        } catch (ex: Exception) {
            Problem(status = Problem.Status.NOT_AUTHORIZED, title = "Not Authorized", detail = ex.message).throwException()
        }
    }

}
