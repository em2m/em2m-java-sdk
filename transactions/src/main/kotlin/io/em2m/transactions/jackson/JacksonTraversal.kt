package io.em2m.transactions.jackson

import com.fasterxml.jackson.databind.JavaType

data class JacksonTraversal(val path: String, val type: JavaType)
