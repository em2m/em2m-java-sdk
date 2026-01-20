package io.em2m.utils

abstract class Fallback<T>(val primary: T, val fallbacks: List<T>) {

    fun <OUT> getOrFallback(mappingFn: (T) -> OUT): Result<OUT> = getOrFallback(primary, fallbacks, mappingFn)

    fun <OUT> getOrThrow(mappingFn: (T) -> OUT): OUT = getOrFallback(mappingFn)
        .getOrThrow()

    fun <OUT> getOrElse(value: OUT, mappingFn: (T) -> OUT): OUT = getOrFallback(mappingFn)
        .getOrElse { _ -> return value }

    fun getOrFalse(mappingFn: (T) -> Boolean): Boolean = getOrElse(false, mappingFn)

    fun <OUT> getOrNull(mappingFn: (T) -> OUT): OUT? = getOrElse(null, mappingFn)

}


fun <T, K> getOrFallback(primary: T, fallbacks: List<T>, mappingFn: (T) -> K): Result<K> {
    val errors = mutableListOf<Throwable>()
    val retObject: K? = try {
        mappingFn(primary)
    } catch (primaryException: Exception) {
        errors.add(primaryException)
        fallbacks.firstNotNullOfOrNull { fallback ->
            try {
                mappingFn(fallback)
            } catch (fallbackException: Exception) {
                errors.add(fallbackException)
                null
            }
        }
    }
    return if (retObject == null) {
        return Result.failure(
            MultiException(
                message = "getOrFallback error",
                errors = errors.toTypedArray()
            ))
    } else {
        Result.success(retObject)
    }
}

open class FallbackPair<T, F>(val primary: T, val fallbacks: List<F>) {

    fun <OUT> getOrFallback(mappingFn: (T) -> OUT, fallbackFn: (F) -> OUT): Result<OUT> = getOrFallback(primary, mappingFn, fallbacks, fallbackFn)

    fun <OUT> getOrThrow(mappingFn: (T) -> OUT, fallbackFn: (F) -> OUT): OUT = getOrFallback(mappingFn, fallbackFn)
        .getOrThrow()

    fun <OUT> getOrElse(value: OUT, mappingFn: (T) -> OUT, fallbackFn: (F) -> OUT): OUT = getOrFallback(mappingFn, fallbackFn)
        .getOrElse { _ -> return value }

    fun getOrFalse(mappingFn: (T) -> Boolean, fallbackFn: (F) -> Boolean): Boolean = getOrElse(false, mappingFn, fallbackFn)

    fun <OUT> getOrNull(mappingFn: (T) -> OUT, fallbackFn: (F) -> OUT): OUT? = getOrElse(null, mappingFn, fallbackFn)

}

fun <T, F, K> getOrFallback(primary: T, mappingFn: (T) -> K, fallbacks: List<F>, fallbackFn: (F) -> K): Result<K> {
    val errors = mutableListOf<Throwable>()
    val retObject: K? = try {
        mappingFn(primary)
    } catch (primaryException: Exception) {
        errors.add(primaryException)
        fallbacks.firstNotNullOfOrNull { fallback ->
            try {
                fallbackFn(fallback)
            } catch (fallbackException: Exception) {
                errors.add(fallbackException)
                null
            }
        }
    }
    return if (retObject == null) {
        return Result.failure(
            MultiException(
                message = "getOrFallback error",
                errors = errors.toTypedArray()
            ))
    } else {
        Result.success(retObject)
    }
}
