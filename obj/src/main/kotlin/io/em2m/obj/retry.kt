package io.em2m.obj

fun <OUT> retry(limit: Int, fn: () -> OUT): OUT {
    var throwable: Throwable = RuntimeException("Could not retry.")
    repeat(limit) {
        val result = runCatching(fn)
        if (result.isSuccess) {
            return result.getOrThrow()
        }
        result.exceptionOrNull()?.let { ex -> throwable = ex }
    }
    throw throwable
}
