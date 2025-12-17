package io.em2m.utils

import java.lang.reflect.Proxy

fun <T: Any> Collection<T>.firstIsClass(clazz: Class<*>): T? {
    return this.filterIsClass(clazz).firstOrNull()
}

fun <T: Any> Collection<T>.filterIsClass(clazz: Class<*>): List<T> {
    if (this.isEmpty()) return emptyList()
    return this.filter { delegate ->
        if (delegate is Proxy) {
            val proxy = this.first() as? Proxy
            val interfaces = proxy?.javaClass?.interfaces ?: arrayOf()
            if (clazz in interfaces) {
                return@filter true
            }
        }
        (delegate as Any).javaClass == clazz
    }
}
