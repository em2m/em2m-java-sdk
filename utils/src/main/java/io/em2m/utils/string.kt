package io.em2m.utils


fun String?.toNonEmptyOrNull(): String? = if (this?.isEmpty() == true) null else this
fun String?.toNonBlankOrNull(): String? = if (this?.isBlank() == true) null else this