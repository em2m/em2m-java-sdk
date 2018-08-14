package io.em2m.actions.xforms


import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.flows.Priorities
import rx.Observable
import javax.servlet.http.Cookie

class CookieTokenTransformer(val cookieName: String, override val priority: Int = Priorities.PRE_AUTHENTICATE) : ActionTransformer {

    override fun call(obs: Observable<ActionContext>): Observable<ActionContext> {

        return obs.doOnNext { context ->
            val cookies = context.environment["cookies"]
            if (cookies is List<*>) {
                cookies.forEach { cookie ->
                    if (cookie is Cookie && cookie.name == cookieName) {
                        context.environment = context.environment.plus("Token" to cookie.value)
                    }
                }
            }
        }
    }

}
