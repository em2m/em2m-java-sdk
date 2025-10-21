package io.em2m.utils

fun Regex.firstIncompleteMatch(baseString: String): String? {
    return this.find(baseString)?.groupValues?.getOrNull(1)
}

fun Regex.firstIncompleteMatchOr(baseString: String, default: String): String {
    return this.firstIncompleteMatch(baseString) ?: default
}
