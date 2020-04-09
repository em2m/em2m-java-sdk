package io.em2m.simplex.std

import io.em2m.simplex.model.*
import io.em2m.utils.coerce
import java.nio.ByteBuffer
import java.util.*

private val decoder = Base64.getDecoder()
private val encoder = Base64.getEncoder()

val StandardBytesConditions = emptyMap<String, ConditionHandler>()

class DecodeBase64 : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        val strValue: String? = value?.coerce()
        return if (strValue != null) {
            decoder.decode(strValue)
        } else value
    }

}

class EncodeBase64 : PipeTransform {

    override fun transform(value: Any?, context: ExprContext): Any? {
        val bytes: ByteArray? = if (value is String) {
            value.toByteArray()
        } else value?.coerce()
        return if (bytes != null) {
            encoder.encodeToString(bytes)
        } else value
    }

}

class EncodeHex : PipeTransform {

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    override fun transform(value: Any?, context: ExprContext): Any? {
        val bytes: ByteArray? = when (value) {
            is String -> value.toByteArray()
            is Short -> ByteBuffer.allocate(java.lang.Short.BYTES).putShort(value).array()
            is Int -> ByteBuffer.allocate(java.lang.Integer.BYTES).putInt(value).array()
            is Long -> ByteBuffer.allocate(java.lang.Long.BYTES).putLong(value).array()
            else -> value?.coerce()
        }
        return bytes?.toHexString()
                ?.removePrefix("00000000")
                ?.removePrefix("0000")
                ?.removePrefix("00")
                ?.let { if (it == "") "00" else it }
                ?: value
    }

}

object Bytes {
    val conditions = BasicConditionResolver(StandardBytesConditions)
    val pipes = BasicPipeTransformResolver(
            mapOf(
                    "decodeBase64" to DecodeBase64(),
                    "encodeBase64" to EncodeBase64(),
                    "encodeHex" to EncodeHex()
            )
    )
}