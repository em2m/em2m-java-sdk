package io.em2m.actions.xforms


import com.auth0.jwt.JWTVerifier
import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformerSupport
import io.em2m.actions.model.Problem
import io.em2m.flows.Priorities
import org.slf4j.LoggerFactory
import rx.Observable

class JwtTransformer(val secretKey: String, val requireAuth: Boolean = false) : ActionTransformerSupport(Priorities.AUTHENTICATE) {

    override fun call(obs: Observable<ActionContext>): Observable<ActionContext> {

        return obs.doOnNext { context ->
            val token = context.environment?.get("Token") as? String
            if (token != null && token.isNotEmpty()) {
                val claims = parseToken(token)
                if (claims != null) {
                    context.claims = claims
                } else {
                    Problem(status = Problem.Status.NOT_AUTHORIZED, title = "Not Authorized").throwException()
                }
            } else if (requireAuth) {
                Problem(status = Problem.Status.NOT_AUTHORIZED, title = "Not Authorized").throwException()
            }
        }
    }

    private fun parseToken(token: String): Map<String, Any?> {
        return try {
            val jwtVerifier = JWTVerifier(secretKey)
            jwtVerifier.verify(token)
        } catch (ex: Exception) {
            LOG.error("Unable to parse/verify the provided JWT token: ", ex)
            emptyMap()
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(JwtTransformer::class.java)
    }
}
