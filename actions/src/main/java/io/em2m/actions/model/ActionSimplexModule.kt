package io.em2m.actions.model

import io.em2m.policy.keys.ClaimsKeyHandler
import io.em2m.policy.keys.EnvironmentKeyHandler
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicSimplexModule
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.PathKeyHandler

class ActionSimplexModule() : BasicSimplexModule() {

    override fun configure(simplex: Simplex) {
        //key(Key("config", "*") to ConfigKeyHandler(config))
        key(Key("ident", "orgPath"), PathKeyHandler("actionContext.scope"))
        key(Key("ident", "role"), ClaimsKeyHandler())
        key(Key("env", "*"), EnvironmentKeyHandler())
        key(Key("claims", "*"), ClaimsKeyHandler())
        key(Key("f", "*"), PathKeyHandler())
        key(Key("field", "*"), PathKeyHandler())
        key(Key("request", "*"), PathKeyHandler("actionContext.request"))
        key(Key("scope", "*"), PathKeyHandler("actionContext.scope"))
    }

}
