package io.em2m.utils

import java.lang.reflect.Proxy

fun <T: Any> Collection<T>.firstIsClass(clazz: Class<*>): T? {
    return this.filterIsClass(clazz).firstOrNull()
}

fun <T: Any> Collection<T>.filterIsClass(clazz: Class<*>): List<T> {
    if (this.isEmpty()) return emptyList()
    return this.filter { delegate -> delegate.isClassOrProxy(clazz) }
}

private fun Any.isClassOrProxy(clazz: Class<*>): Boolean {
    if (this is Proxy) {
        val interfaces = this.javaClass.interfaces ?: arrayOf()
        if (clazz in interfaces) {
            return true
        }
    }
    return this.javaClass == clazz
}
