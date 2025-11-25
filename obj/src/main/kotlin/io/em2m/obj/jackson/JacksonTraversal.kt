package io.em2m.obj.jackson

import com.fasterxml.jackson.databind.JavaType

data class JacksonTraversal(val path: String, val type: JavaType)
