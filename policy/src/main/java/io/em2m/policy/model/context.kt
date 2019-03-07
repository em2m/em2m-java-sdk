package io.em2m.policy.model

import io.em2m.simplex.model.KeyResolver
import java.util.*


class Claims(initial: Map<String, Any?> = emptyMap()) {

    val map = HashMap(initial)

    fun get(key: String): Any? {
        return map[key]
    }

    fun put(key: String, value: Any) {
        map[key] = value
    }

    fun contains(key: String): Boolean {
        return map.contains(key)
    }

    var sub: String?
        get() = map["sub"] as String?
        set(value) {
            map["sub"] = value
        }

    var exp: Date?
        get() = map["expr"] as? Date
        set(value) {
            map["date"] = value
        }

    var iss: String?
        get() = map["iss"] as String?
        set(value) {
            map["iss"] = value
        }

    var aud: String?
        get() = map["aud"] as String?
        set(value) {
            map["aud"] = value
        }

    var nbf: Date?
        get() = map["nbf"] as? Date
        set(value) {
            map["nbf"] = value
        }

    var iat: Date?
        get() = map["iat"] as? Date
        set(value) {
            map["iat"] = value
        }

    var jti: String?
        get() = map["jti"] as String?
        set(value) {
            map["jti"] = value
        }

    var tid: String?
        get() = map["tid"] as String?
        set(value) {
            map["tid"] = value
        }

    var did: String?
        get() = map["did"] as String?
        set(value) {
            map["did"] = value
        }

    var bid: String?
        get() = map["bid"] as String?
        set(value) {
            map["bid"] = value
        }

    var org: String?
        get() = map["org"] as String?
        set(value) {
            map["org"] = value
        }

    var orgPath: List<String>?
        get() = map["orgPath"] as? List<String>
        set(value) {
            map["orgPath"] = value
        }

    var roles: List<String>
        get() = map["roles"] as? List<String> ?: emptyList()
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


