package io.em2m.search.es.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.em2m.search.es.EsApi
import io.em2m.search.es.jackson.EsVersionDeserializer
import io.em2m.search.es.jackson.EsVersionSerializer
import io.em2m.search.es8.Es8Api

@JsonSerialize(using= EsVersionSerializer::class)
@JsonDeserialize(using = EsVersionDeserializer::class)
data class EsVersion(val number: String) {
    private val splitNumber = number.split(".")
    @JsonIgnore
    val major: Int? = splitNumber.getOrNull(0)?.toIntOrNull()
    @JsonIgnore
    val minor: Int? = splitNumber.getOrNull(1)?.toIntOrNull()
    @JsonIgnore
    val snapshot: Int? = splitNumber.getOrNull(2)?.toIntOrNull()

    override fun toString(): String = listOfNotNull(major, minor, snapshot)
        .joinToString(".")

    override fun equals(other: Any?): Boolean {
        if (other is String) return other == this.number
        if (other !is EsVersion) return false
        return this.toLong() == other.toLong()
    }

    override fun hashCode(): Int = number.hashCode()

    @JsonIgnore
    fun getApi(): Class<*> {
        return when (this.major) {
            2 -> EsApi::class.java
            8 -> Es8Api::class.java
            else -> TODO("Unsupported version")
        }
    }

    operator fun compareTo(other: EsVersion): Int {
        return this.toLong().compareTo(other.toLong())
    }

    operator fun compareTo(other: String): Int {
        return this.toLong().compareTo(EsVersion(other))
    }

    operator fun compareTo(other: Long): Int {
        return this.toLong().compareTo(other)
    }

    operator fun compareTo(other: Int): Int {
        return this.toLong().compareTo(other)
    }

    fun toLong(): Long {
        val factor = 1000L
        var ret = 0L
        ret += (snapshot?.toLong() ?: 0L)
        ret += (minor?.toLong() ?: 0L) * factor
        ret += (major?.toLong() ?: 0L) * factor * factor
        return ret
    }

    infix fun isLike(other: EsVersion): Boolean {
        return this.major == other.major
    }

    companion object {
        val ES2 = EsVersion("2")
        val ES8 = EsVersion("8")

        val DEFAULT: EsVersion = ES2
        val VALUES = listOf(ES2, ES8)

        // extensions
        operator fun String.compareTo(other: EsVersion): Int = other.compareTo(this)
        operator fun Int.compareTo(other: EsVersion): Int = other.compareTo(this)
        operator fun Long.compareTo(other: EsVersion): Int = other.compareTo(this)
    }

}
