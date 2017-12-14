package io.em2m.actions.model

import java.io.InputStream
import java.util.*
import javax.servlet.http.Part

data class ActionContext(val actionName: String,
                         val inputStream: InputStream? = null, val parts: List<Part> = emptyList(),
                         var claims: Map<String, Any?> = emptyMap(), var environment: Map<String, Any?> = emptyMap(), var resource: String? = null,
                         val scope: MutableMap<String, Any?> = HashMap(),
                         var requestId: String = UUID.randomUUID().toString(),
                         var request: Any? = null, val response: Response = Response(), var multipart: MultipartData? = null)
