package io.em2m.utils

import com.typesafe.config.Config


fun Config.safeGet(path: String): Any? {
    return try {
        this.getValue(path).unwrapped()
    } catch (ex: Exception) {
        null
    }
}