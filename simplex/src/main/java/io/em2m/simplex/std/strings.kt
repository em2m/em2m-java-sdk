package io.em2m.simplex.std

import io.em2m.simplex.model.*
import io.em2m.utils.coerce
import java.util.regex.Matcher


class UpperCasePipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value is List<*>) {
            value.map { it?.toString()?.toUpperCase() }
        } else if (value is Array<*>) {
            value.map { it?.toString()?.toUpperCase() }
        } else {
            value?.toString()?.toUpperCase()
        }
    }
}


class CapitalizePipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value is List<*>) {
            value.map { it?.toString()?.capitalize() }
        } else if (value is Array<*>) {
            value.map { it?.toString()?.capitalize() }
        } else {
            value?.toString()?.capitalize()
        }
    }
}

class TrimPipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value is List<*>) {
            value.map { it?.toString()?.trim() }
        } else if (value is Array<*>) {
            value.map { it?.toString()?.trim() }
        } else {
            value?.toString()?.trim()
        }
    }
}

class AppendPipe : PipeTransform {
    var text = ""

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            text = args[0]
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value != null) {
            value.toString() + text
        } else {
            return null
        }
    }
}

class JoinPipe : PipeTransform {

    var separator = ", "

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            separator = args[0]
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.joinToString(separator)
            is Array<*> -> value.joinToString(separator)
            else -> value
        }
    }
}


open class SingleStringHandler(private val op: (String?, String?) -> Boolean) : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        val keyString: String? = if (keyValue is List<*>) {
            keyValue[0].coerce()
        } else keyValue.coerce()
        val valueString: String? = if (conditionValue is List<*>) {
            conditionValue[0].coerce()
        } else conditionValue.coerce()

        return op(keyString, valueString)
    }
}

open class ForAnyStringHandler(private val op: (String?, String?) -> Boolean) : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        val keyList = keyValue as? List<*> ?: listOf(keyValue)
        val valList = conditionValue as? List<*> ?: listOf(conditionValue)

        var result = false

        keyList.forEach { first ->
            valList.forEach { second ->
                if (op(first.coerce(), second.coerce())) {
                    result = true
                }
            }
        }
        return result
    }
}

open class ForAllStringHandler(private val op: (String?, String?) -> Boolean) : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        val keyList = if (keyValue == null) {
            emptyList<Any>()
        } else keyValue as? List<*> ?: listOf(keyValue)
        val valList = conditionValue as? List<*> ?: listOf(conditionValue)

        return keyList.fold(true) { result, key ->
            result && valList.any { op(key.coerce(), it.coerce()) }
        }
    }
}

fun stringLike(k: String?, v: String?): Boolean {
    if (k == null && v == null) return true
    if (k == null || v == null) return false
    val replaced = v.replace("?", "_QUESTION_MARK_").replace("*", "_STAR_")
    val escaped = Matcher.quoteReplacement(replaced).replace("_QUESTION_MARK_", ".?").replace("_STAR_", ".*")
    val pattern = Matcher.quoteReplacement(escaped)
    val regex = Regex(pattern)
    return regex.matches(k)
}

class StringEquals : SingleStringHandler({ k, v -> k == v })
class StringNotEquals : SingleStringHandler({ k, v -> k != v })
class StringEqualsIgnoreCase : SingleStringHandler({ k, v -> k.equals(v, ignoreCase = true) })
class StringNotEqualsIgnoreCase : SingleStringHandler({ k, v -> !k.equals(v, ignoreCase = true) })
class StringLike : SingleStringHandler({ k, v -> stringLike(k, v) })
class StringNotLike : SingleStringHandler({ k, v -> !stringLike(k, v) })

class ForAnyStringEquals : ForAnyStringHandler({ k, v -> k == v })
class ForAnyStringNotEquals : ForAnyStringHandler({ k, v -> k != v })
class ForAnyStringEqualsIgnoreCase : ForAnyStringHandler({ k, v -> k.equals(v, ignoreCase = true) })
class ForAnyStringNotEqualsIgnoreCase : ForAnyStringHandler({ k, v -> !k.equals(v, ignoreCase = true) })
class ForAnyStringLike : ForAnyStringHandler({ k, v -> stringLike(k, v) })
class ForAnyStringNotLike : ForAnyStringHandler({ k, v -> !stringLike(k, v) })

class ForAllStringEquals : ForAllStringHandler({ k, v -> k == v })
class ForAllStringNotEquals : ForAllStringHandler({ k, v -> k != v })
class ForAllStringEqualsIgnoreCase : ForAllStringHandler({ k, v -> k.equals(v, ignoreCase = true) })
class ForAllStringNotEqualsIgnoreCase : ForAllStringHandler({ k, v -> !k.equals(v, ignoreCase = true) })
class ForAllStringLike : ForAllStringHandler({ k, v -> stringLike(k, v) })
class ForAllStringNotLike : ForAllStringHandler({ k, v -> !stringLike(k, v) })

val StandardStringConditions = mapOf(

        "StringEquals" to StringEquals(),
        "StringNotEquals" to StringNotEquals(),
        "StringEqualsIgnoreCase" to StringEqualsIgnoreCase(),
        "StringNotEqualsIgnoreCase" to StringNotEqualsIgnoreCase(),
        "StringLike" to StringLike(),
        "StringNotLike" to StringNotLike(),

        "ForAllValues:StringEquals" to ForAllStringEquals(),
        "ForAllValues:StringNotEquals" to ForAllStringNotEquals(),
        "ForAllValues:StringEqualsIgnoreCase" to ForAllStringEqualsIgnoreCase(),
        "ForAllValues:StringNotEqualsIgnoreCase" to ForAllStringNotEqualsIgnoreCase(),
        "ForAllValues:StringLike" to ForAllStringLike(),
        "ForAllValues:StringNotLike" to ForAllStringNotLike(),

        "ForAnyValue:StringEquals" to ForAnyStringEquals(),
        "ForAnyValue:StringNotEquals" to ForAnyStringNotEquals(),
        "ForAnyValue:StringEqualsIgnoreCase" to ForAnyStringEqualsIgnoreCase(),
        "ForAnyValue:StringNotEqualsIgnoreCase" to ForAnyStringNotEqualsIgnoreCase(),
        "ForAnyValue:StringLike" to ForAnyStringLike(),
        "ForAnyValue:StringNotLike" to ForAnyStringNotLike()
)

object Strings {

    val pipes = BasicPipeTransformResolver(mapOf(
            "upperCase" to UpperCasePipe(),
            "capitalize" to CapitalizePipe(),
            "trim" to TrimPipe(),
            "append" to AppendPipe(),
            "join" to JoinPipe()))
    val keys = BasicKeyResolver()
    val conditions = BasicConditionResolver(StandardStringConditions)

}