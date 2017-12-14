package io.em2m.policy.keys

import io.em2m.policy.model.PolicyContext
import io.em2m.simplex.model.ExprContext
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.KeyHandlerSupport


class RoleCustomData : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return null
    }
}

class EnvironmentKeyHandler : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return PolicyContext(context).environment.get(key.name)
    }

}

class ClaimsKeyHandler : KeyHandlerSupport() {

    override fun call(key: Key, context: ExprContext): Any? {
        return PolicyContext(context).claims.get(key.name)
    }

}
