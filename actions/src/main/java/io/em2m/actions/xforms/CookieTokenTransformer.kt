package io.em2m.actions.xforms


import io.em2m.actions.model.ActionContext
import io.em2m.actions.model.ActionTransformer
import io.em2m.flows.Priorities
import javax.servlet.http.Cookie

class CookieTokenTransformer(val cookieName: String, override val priority: Int = Priorities.PRE_AUTHENTICATE) : ActionTransformer {

    override fun doOnNext(ctx: ActionContext) {
        val cookies = ctx.environment["cookies"]
        if (cookies is List<*>) {
            cookies.forEach { cookie ->
                if (cookie is Cookie && cookie.name == cookieName) {
                    ctx.environment["Token"] = cookie.value
                }
            }
        }
    }

}
