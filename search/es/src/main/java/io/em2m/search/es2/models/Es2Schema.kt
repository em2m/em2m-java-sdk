package io.em2m.search.es2.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es.models.EsStatMapping
import io.em2m.search.es2.operations.es2LoadSchema
import io.em2m.search.es2.operations.es2SaveSchema
import java.io.File

@Deprecated("Use Es8Schema")
data class Es2Schema(
    val indicesToAliases:   Map<String, Set<String>>,
    val indicesToSettings:  Map<String, Any?>,
    val indicesToStats:     Map<String, EsStatMapping?>,
    val aliasesToMappings:  Map<String, Set<Es2Mapping>>,
    val indicesToMappings:  Map<String, Es2Mapping>) {

    fun save(to: File, mapper: ObjectMapper = jacksonObjectMapper()) {
        es2SaveSchema(this, to, mapper)
    }

    companion object {

        fun load(from: File, mapper: ObjectMapper = jacksonObjectMapper()): Es2Schema {
            return es2LoadSchema(from, mapper)
        }

    }

}
