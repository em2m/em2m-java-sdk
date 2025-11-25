package io.em2m.search.es2.operations

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.ALIASES_FILE_NAME
import io.em2m.search.MAPPING_FILE_NAME
import io.em2m.search.SETTINGS_FILE_NAME
import io.em2m.search.STATS_FILE_NAME
import io.em2m.search.es2.models.Es2Schema
import java.io.File

fun es2SaveSchema(es2Schema: Es2Schema, es2Directory: File, mapper: ObjectMapper = jacksonObjectMapper()) {
    if (es2Directory.isFile) {
        throw IllegalArgumentException()
    }

    val prettyWriter = mapper.writerWithDefaultPrettyPrinter()

    es2Schema.aliasesToMappings.forEach { (alias, mappings) ->
        val aliasDirectory = File(es2Directory, alias)
        aliasDirectory.mkdirs()
        mappings.forEach { mapping ->
            val typeDirectory = File(aliasDirectory, mapping.type)
            typeDirectory.mkdirs()
            val indexDirectory = File(typeDirectory, mapping.index)
            indexDirectory.mkdirs()

            val mappingFile = File(indexDirectory, "${MAPPING_FILE_NAME}.json")
            mappingFile.writeText(prettyWriter.writeValueAsString(mapping.properties), Charsets.UTF_8)

            val aliases = es2Schema.indicesToAliases[mapping.index]
            val aliasesFile = File(indexDirectory, "${ALIASES_FILE_NAME}.json")
            aliasesFile.writeText(prettyWriter.writeValueAsString(aliases), Charsets.UTF_8)

            val settings = es2Schema.indicesToSettings[mapping.index]
            val settingsFile = File(indexDirectory, "${SETTINGS_FILE_NAME}.json")
            settingsFile.writeText(prettyWriter.writeValueAsString(settings), Charsets.UTF_8)

            val stats = es2Schema.indicesToStats[mapping.index]
            val statsFile = File(indexDirectory, "${STATS_FILE_NAME}.json")
            statsFile.writeText(prettyWriter.writeValueAsString(stats), Charsets.UTF_8)
        }
    }
}
