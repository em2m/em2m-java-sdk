package io.em2m.actions.model

data class Response(var statusCode: Int = 200, var entity: Any? = null, var error: ActionError? = null, val headers: MutableMap<String, String> = HashMap()) {

    init {
        error?.apply {
            this@Response.statusCode = status
        }
    }

    fun error(error: ActionError) {
        this.error = error
        this.statusCode = error.status
    }

}
