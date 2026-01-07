package io.em2m.search.es2.operations

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.*
import io.em2m.search.es.models.EsStatMapping
import io.em2m.search.es2.models.Es2Mapping
import io.em2m.search.es2.models.Es2Schema
import io.em2m.utils.coerce
import java.io.File
import java.io.FileFilter

fun es2LoadSchema(from: File, mapper: ObjectMapper = jacksonObjectMapper()): Es2Schema {
    if (!from.exists() || !from.isDirectory) {
        throw IllegalArgumentException("From file must be a directory.")
    }

    val indexDirectories = from.walkBottomUp().filter(File::isDirectory).filter { directory ->
        val children = directory.listFiles { pathname -> pathname?.isFile == true } ?: emptyArray()
        val fileNames = children.map(File::getName)
        ES_SCHEMA_FILE_NAMES.all { it in fileNames }
    }.toSet()

    val indicesToMappings:  MutableMap<String, Es2Mapping>                  = mutableMapOf()
    val aliasesToIndices:   MutableMap<String, MutableSet<String>>          = mutableMapOf()

    val indicesToAliases:   MutableMap<String, Set<String>>                 = mutableMapOf()
    val indicesToSettings:  MutableMap<String, Any?>                        = mutableMapOf()
    val indicesToStats:     MutableMap<String, EsStatMapping?>              = mutableMapOf()
    val aliasesToMappings:  MutableMap<String, MutableSet<Es2Mapping>>      = mutableMapOf()

    indexDirectories.forEach { indexDirectory ->
        val indexName = indexDirectory.name
        val indexType = indexDirectory.parentFile.name

        val mappingFile     = File(indexDirectory, "${MAPPING_FILE_NAME}.json")
        val properties      = mapper.readTree(mappingFile).coerce() ?: mutableMapOf<String, Any?>()
        val mapping         = Es2Mapping(indexName, indexType, properties)

        val settingsFile    = File(indexDirectory, "${SETTINGS_FILE_NAME}.json")
        val settings        = mapper.readTree(settingsFile)

        val aliasesFile     = File(indexDirectory, "${ALIASES_FILE_NAME}.json")
        val aliases         = mapper.readValue(aliasesFile, List::class.java)
            .mapNotNullTo(mutableSetOf()) { it?.toString()}

        val statsFile       = File(indexDirectory, "${STATS_FILE_NAME}.json")
        val stats           = mapper.readTree(statsFile).coerce() ?: mutableMapOf<String, Any?>()

        indicesToMappings[indexName]    = mapping

        indicesToAliases[indexName]     = aliases
        indicesToSettings[indexName]    = settings
        indicesToStats[indexName]       = stats
    }

    val aliases = indicesToAliases.values.flatten().toSet()
    indicesToAliases.forEach { (index, mappedAliases) ->
        mappedAliases.forEach { alias ->
            val indices = aliasesToIndices[alias] ?: mutableSetOf()
            indices.add(index)
            aliasesToIndices[alias] = indices
        }
    }

    aliases.forEach { alias -> aliasesToMappings[alias] = mutableSetOf() }

    return Es2Schema(indicesToAliases, indicesToSettings, indicesToStats, aliasesToMappings, indicesToMappings)
}
