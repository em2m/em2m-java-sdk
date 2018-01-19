package io.em2m.policy.model

import io.em2m.simplex.model.KeyResolver
import java.util.*


class Claims(initial: Map<String, Any?>) {

    val map = HashMap(initial)

    init {
        map.putIfAbsent("sub", null)
        map.putIfAbsent("exp", null)
        map.putIfAbsent("iss", null)
        map.putIfAbsent("aud", null)
        map.putIfAbsent("nbf", null)
        map.putIfAbsent("iat", null)
        map.putIfAbsent("jti", null)
        map.putIfAbsent("tid", null)
        map.putIfAbsent("did", null)
        map.putIfAbsent("bid", null)
        map.putIfAbsent("org", null)
        map.putIfAbsent("orgPath", null)
    }

    fun get(key: String): Any? {
        return map[key]
    }

    fun put(key: String, value: Any) {
        map[key] = value
    }

    fun contains(key: String): Boolean {
        return map.contains(key)
    }

    var sub: String? by map
    var exp: Date? by map
    var iss: String? by map
    var aud: String? by map
    var nbf: Date? by map
    var iat: Date? by map
    var jti: String? by map
    var tid: String? by map
    var did: String? by map
    var bid: String? by map
    var org: String? by map
    var orgPath: List<String>? by map

    var roles: List<String>
        get() = map["roles"] as? List<String>? ?: emptyList()
        set(value) {
            map["roles"] = value
        }
}


class Environment(initial: Map<String, Any?>) {

    val map = HashMap(initial)

    init {
        map.putIfAbsent("Token", null)
    }

    fun get(key: String): Any? {
        return map[key]
    }

    fun put(key: String, value: Any?) {
        map[key] = value
    }

    fun contains(key: String): Boolean {
        return map.contains(key)
    }

    var Token: String? by map

}

open class PolicyContext(val map: Map<String, Any?>) {
    val claims: Claims by map
    val environment: Environment by map
    val resource: String? by map
    val keys: KeyResolver? by map

    constructor(map: Map<String, Any?>, claims: Claims, environment: Environment, resource: String?, keys: KeyResolver? = null) :
            this(map.plus(mapOf("claims" to claims, "environment" to environment, "resource" to resource, "keys" to keys)))

    constructor(claims: Claims, environment: Environment, resource: String?, keys: KeyResolver? = null) :
            this(emptyMap(), claims, environment, resource, keys)
}


