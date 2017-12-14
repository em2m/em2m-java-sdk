package io.em2m.conveyor.actions.model

data class Response(var statusCode: Int = 200, var entity: Any? = null, val headers: MutableMap<String, String> = HashMap())
