package io.em2m.simplex.std

import com.fasterxml.jackson.databind.node.ArrayNode
import io.em2m.simplex.model.*
import io.em2m.utils.coerce
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.Matcher


class UpperCasePipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is List<*> -> value.map { it?.toString()?.toUpperCase() }
            is Array<*> -> value.map { it?.toString()?.toUpperCase() }
            is ArrayNode -> value.map { it?.toString()?.toUpperCase() }
            else -> value?.toString()?.toUpperCase()
        }
    }
}


class CapitalizePipe : PipeTransform {
    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value is Iterable<*>) {
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
        return if (value is Iterable<*>) {
            value.map { it?.toString()?.trim() }
        } else if (value is Array<*>) {
            value.map { it?.toString()?.trim() }
        } else {
            value?.toString()?.trim()
        }
    }
}

class FormatPhonePipe : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value is Iterable<*>) {
            value.map { phoneFormat(it) }
        } else if (value is Array<*>) {
            value.map { phoneFormat(it) }
        } else {
            phoneFormat(value)
        }
    }

    fun phoneFormat(value: Any?): Any? {
        val phoneNumber = value?.toString()?.trim() ?: return null
        if (phoneNumber.length == 10) {
            return "(" + phoneNumber.substring(0,3) + ") " + phoneNumber.substring(3,6) + "-" + phoneNumber.substring(6,10)
        } else {
            return phoneNumber
        }
    }
}

class RemoveCharsPipe : PipeTransform {

    var chars = ""


    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value is Iterable<*>) {
            value.map { removeChar(it) }
        } else if (value is Array<*>) {
            value.map { removeChar(it) }
        } else {
            removeChar(value)
        }
    }

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            chars = args[0]
        }
    }

    fun removeChar(value: Any?): Any? {
        var phoneNumber = value?.toString()

        for (c in chars) {
            phoneNumber = phoneNumber?.replace(c.toString(), "")
        }
        return phoneNumber
    }
}

class UrlEncodePipe : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value is Iterable<*>) {
            value.map { encode(it) }
        } else if (value is Array<*>) {
            value.map { encode(it) }
        } else {
            encode(value)
        }
    }

    fun encode(value: Any?): Any? {
        return value?.toString()?.let { URLEncoder.encode(it, "UTF-8") } ?: value
    }

}

class UrlDecodePipe : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value is Iterable<*>) {
            value.map { decode(it) }
        } else if (value is Array<*>) {
            value.map { decode(it) }
        } else {
            decode(value)
        }
    }

    fun decode(value: Any?): Any? {
        return value?.toString()?.let { URLDecoder.decode(it, "UTF-8") } ?: value
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

class PrependPipe : PipeTransform {
    var text = ""

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            text = args[0]
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value != null) {
            text + value.toString()
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
            is Iterable<*> -> value.joinToString(separator)
            is Array<*> -> value.joinToString(separator)
            else -> value
        }
    }
}

class EmptyToNull : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        return if (value is Iterable<*>) {
            value.map {
                if ((it as? String)?.isEmpty() == true) null else it
            }
        } else if (value is Array<*>) {
            value.map {
                if ((it as? String)?.isEmpty() == true) null else it
            }
        } else {
            if ((value as? String)?.isEmpty() == true) null else value
        }
    }

}

class Split : PipeTransform {

    var separator = ","

    override fun args(args: List<String>) {
        if (args.isNotEmpty()) {
            separator = args[0]
        }
    }

    override fun transform(value: Any?, context: ExprContext): Any? {
        return when (value) {
            is Iterable<*> -> {
                value.flatMap { it?.toString()?.split(separator) ?: emptyList() }
            }
            is Array<*> -> {
                value.flatMap { it?.toString()?.split(separator) ?: emptyList() }
            }
            else -> {
                value?.toString()?.split(separator)
            }
        }
    }

}

open class SingleStringHandler(private val op: (String?, String?) -> Boolean) : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        val keyString: String? = if (keyValue is Iterable<*>) {
            keyValue.firstOrNull()?.coerce()
        } else keyValue?.coerce()
        val valueString: String? = if (conditionValue is Iterable<*>) {
            conditionValue.firstOrNull()?.coerce()
        } else conditionValue?.coerce()

        return op(keyString, valueString)
    }
}

open class ForAnyStringHandler(private val op: (String?, String?) -> Boolean) : ConditionHandler {

    override fun test(keyValue: Any?, conditionValue: Any?): Boolean {
        val keyList = keyValue as? Iterable<*> ?: listOf(keyValue)
        val valList = conditionValue as? Iterable<*> ?: listOf(conditionValue)

        var result = false

        keyList.forEach { first ->
            valList.forEach { second ->
                if (op(first?.coerce(), second?.coerce())) {
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
        } else keyValue as? Iterable<*> ?: listOf(keyValue)
        val valList = conditionValue as? Iterable<*> ?: listOf(conditionValue)

        return keyList.fold(true) { result, key ->
            result && valList.any { op(key?.coerce(), it?.coerce()) }
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
class StringNotEquals : Not(StringEquals())
class StringEqualsIgnoreCase : SingleStringHandler({ k, v -> k.equals(v, ignoreCase = true) })
class StringNotEqualsIgnoreCase : Not(StringEqualsIgnoreCase())
class StringLike : SingleStringHandler({ k, v -> stringLike(k, v) })
class StringNotLike : Not(StringLike())

class ForAnyStringEquals : ForAnyStringHandler({ k, v -> k == v })
class ForAnyStringNotEquals : Not(ForAnyStringEquals())
class ForAnyStringEqualsIgnoreCase : ForAnyStringHandler({ k, v -> k.equals(v, ignoreCase = true) })
class ForAnyStringNotEqualsIgnoreCase : Not(ForAnyStringEqualsIgnoreCase())
class ForAnyStringLike : ForAnyStringHandler({ k, v -> stringLike(k, v) })
class ForAnyStringNotLike : Not(ForAnyStringLike())

class ForAllStringEquals : ForAllStringHandler({ k, v -> k == v })
class ForAllStringNotEquals : Not(ForAllStringEquals())
class ForAllStringEqualsIgnoreCase : ForAllStringHandler({ k, v -> k.equals(v, ignoreCase = true) })
class ForAllStringNotEqualsIgnoreCase : Not(ForAllStringEqualsIgnoreCase())
class ForAllStringLike : ForAllStringHandler({ k, v -> stringLike(k, v) })
class ForAllStringNotLike : Not(ForAllStringLike())

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

    val pipes = BasicPipeTransformResolver()
            .transform("upperCase") { UpperCasePipe() }
            .transform("capitalize") { CapitalizePipe() }
            .transform("trim") { TrimPipe() }
            .transform("append") { AppendPipe() }
            .transform("prepend") { PrependPipe() }
            .transform("join") { JoinPipe() }
            .transform("emptyToNull") { EmptyToNull() }
            .transform("split") { Split() }
            .transform("urlEncode", UrlEncodePipe())
            .transform("urlDecode", UrlDecodePipe())
            .transform("formatPhone", FormatPhonePipe())
            .transform("removeChars", RemoveCharsPipe())
    val keys = BasicKeyResolver()
    val conditions = BasicConditionResolver(StandardStringConditions)

}
