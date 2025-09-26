package io.em2m.utils

import java.nio.charset.Charset

private val DEFAULT_CHARSET = Charsets.UTF_8
private val CHARSET_REGEX = "(?i)charset\\s*=\\s*['\"]?([^;'\"]+)".toRegex()

fun parseCharset(contentType: String?): Charset {
    if (contentType == null) return DEFAULT_CHARSET
    val charsetString = CHARSET_REGEX.firstIncompleteMatchOr(contentType, "UTF-8")
    return try {
        Charset.forName(charsetString)
    } catch (_: Exception) {
        DEFAULT_CHARSET
    }
}
