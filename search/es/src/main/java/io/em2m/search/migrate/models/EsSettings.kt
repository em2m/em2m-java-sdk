package io.em2m.search.migrate.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es.models.EsVersion
import io.em2m.search.es2.models.Es2Settings
import io.em2m.search.es8.models.Es8Settings
import io.em2m.search.migrate.toLegacy
import io.em2m.search.migrate.toModern
import io.em2m.simplex.evalPaths
import io.em2m.utils.coerce
import io.em2m.utils.coerceNonNull

data class EsSettings(@Deprecated("Prefer newer settings models.") val old: Es2Settings, val new: Es8Settings) {


    companion object {

        fun getVersion(from: ObjectNode): EsVersion {
            val createdString = from.evalPaths(
                "settings.index.version.created_string",
                "index.version.created_string"
                ).firstOrNull()?.toString()
            return createdString?.let {
                EsVersion(it)
            } ?: EsVersion.DEFAULT
        }

        fun fromObjectNode(from: ObjectNode, mapper: ObjectMapper = jacksonObjectMapper()): EsSettings {
            val version = getVersion(from)
            val old: Es2Settings
            val new: Es8Settings
            if (version isLike EsVersion.ES2) {
                val asEs2Settings = from.coerceNonNull(Es2Settings(), objectMapper = mapper)
                old = asEs2Settings
                new = old.toModern()
            } else if (version isLike EsVersion.ES8) {
                val asEs8Settings = from.coerceNonNull(Es8Settings(), objectMapper = mapper)
                new = asEs8Settings
                old = new.toLegacy()
            } else {
                val oldish = from.coerce<Es2Settings>(objectMapper = mapper)
                val newish = from.coerce<Es8Settings>(objectMapper = mapper)
                if (oldish != null) {
                    old = oldish
                    new = old.toModern()
                } else if (newish != null) {
                    new = newish
                    old = new.toLegacy()
                } else {
                    throw IllegalArgumentException("Expecting Es2Settings or Es8Settings compatible objects")
                }
            }
            return EsSettings(old, new)
        }

    }

}
