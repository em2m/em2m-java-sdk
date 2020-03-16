package io.em2m.actions.model

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class Problem(val type: String? = null,
              val title: String,
              val status: Int = 500,
              val detail: String? = null,
              val instance: String? = null,
              ext: Map<String, Any?> = HashMap()) {

    @JsonIgnore
    val extensions: MutableMap<String, Any?> = HashMap()

    @JsonAnyGetter
    fun anyGetter(): Map<String, Any?> {
        return extensions
    }

    @JsonAnySetter
    fun setAny(key: String, value: Any?): Problem {
        extensions[key] = value
        return this
    }

    fun throwException(): Nothing {
        throw ProblemException(this)
    }

    init {
        extensions.putAll(ext)
    }

    companion object {

        fun notAuthorized(title: () -> Any? = { "Not Authorized" }, detail: () -> Any? = {}, ext: Map<String, Any?> = emptyMap()): Nothing {
            Problem(title = title().toString(), status = Status.NOT_AUTHORIZED, detail = detail().toString(), ext = ext).throwException()
        }

        fun badRequest(title: () -> Any? = { "Bad Request" }, detail: () -> Any? = {}, ext: Map<String, Any?> = emptyMap()): Nothing {
            Problem(title = title().toString(), status = Status.BAD_REQUEST, detail = detail().toString(), ext = ext).throwException()
        }

        fun notFound(title: () -> Any? = { "Not Found" }, detail: () -> Any? = {}, ext: Map<String, Any?> = emptyMap()): Nothing {
            Problem(title = title().toString(), status = Status.NOT_FOUND, detail = detail().toString(), ext = ext).throwException()
        }

        fun conflict(title: () -> Any? = { "Conflict" }, detail: () -> Any? = {}, ext: Map<String, Any?> = emptyMap()): Nothing {
            Problem(title = title().toString(), status = Status.CONFLICT, detail = detail()?.toString(), ext = ext).throwException()
        }

        fun unexpectedError(title: () -> Any? = { "Unexpected Error" }, detail: () -> Any? = {}, ext: Map<String, Any?> = emptyMap()): Nothing {
            Problem(title = title().toString(), status = Status.INTERNAL_SERVER_ERROR, detail = detail().toString(), ext = ext).throwException()
        }

        fun <T> valueOrNotFound(value: T?, title: () -> Any? = { "Not Found" }, detail: () -> Any? = {}, ext: Map<String, Any?> = emptyMap()): T {
            return value ?: notFound(title, detail, ext)
        }

        fun <T> valueOrConflict(value: T?, title: () -> Any? = { "Not Found" }, detail: () -> Any? = {}, ext: Map<String, Any?> = emptyMap()): T {
            return value ?: conflict(title, detail, ext)
        }

        fun convert(throwable: Throwable): Problem {
            return when (throwable) {
                is ProblemException -> throwable.problem
                is FlowNotFound -> Problem(status = Status.NOT_FOUND, title = "Action ${throwable.name} not found.")
                is IllegalStateException -> Problem(status = Status.CONFLICT, title = "Conflict", detail = throwable.message)
                is IllegalArgumentException -> Problem(status = Status.BAD_REQUEST, title = "Bad Request", detail = throwable.message)
                else -> Problem(status = Status.INTERNAL_SERVER_ERROR, title = "Internal Server Error", detail = throwable.message)
            }
        }
    }

    object Status {
        val BAD_REQUEST = 400
        val NOT_AUTHORIZED = 401
        val FORBIDDEN = 403
        val NOT_FOUND = 404
        val CONFLICT = 409
        val INTERNAL_SERVER_ERROR = 500
    }
}
