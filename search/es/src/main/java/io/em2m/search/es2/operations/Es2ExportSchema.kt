package io.em2m.search.es2.operations

import io.em2m.search.es2.Es2Api
import io.em2m.search.es2.models.Es2Index
import io.em2m.search.es2.models.Es2Mapping
import io.em2m.search.es2.models.Es2Schema
import io.em2m.simplex.evalPath

fun Es2Api.exportSchema(indexWhitelist: List<String> = emptyList()): Es2Schema {
    return es2ExportSchema(this, indexWhitelist)
}

private fun es2ExportSchema(es2Client: Es2Api,
                            indexWhitelist: List<String> = emptyList()): Es2Schema {
    var indices = es2Client.getIndices()
    var aliases: Map<String, List<String>> = es2Client.getAliases().fields().asSequence().associate { (index, fields) ->
        index to fields.get("aliases").fieldNames().asSequence().toList()
    }
    var mappings = es2Client.getAllMappings()
    val stats = es2Client.getStats()

    if (indexWhitelist.isNotEmpty()) {
        fun String.isWhitelisted(indexAliases: List<String> = aliases[this] ?: emptyList()): Boolean {
            if (this in indexWhitelist) return true
            if (indexAliases.any { alias -> alias in indexWhitelist}) return true
            return false
        }
        indices = indices.filter { es2Index: Es2Index -> es2Index.index.isWhitelisted() }
        aliases = aliases.filter { (index: String, aliases: List<String>) -> index.isWhitelisted(aliases) }
        mappings = mappings.filterKeys(String::isWhitelisted)
    }

    val indicesToAliases = mutableMapOf<String, MutableSet<String>>()
    indices.forEach { es2Index -> indicesToAliases[es2Index.index] = mutableSetOf() }

    // val indexNames = indicesToAliases.keys

    val indicesToSettings = indicesToAliases.keys.associateWith { index ->
        (es2Client.getSettings(index) as? Map<*, *>)
            ?.evalPath("${index}.settings.index")
    }

    val indicesToStats = indicesToAliases.keys.associateWith { index -> stats[index] }

    val aliasesToMappings = mutableMapOf<String, MutableSet<Es2Mapping>>()

    aliases.forEach { (index, aliasNames) ->
        aliasNames.forEach { alias ->
            val matchingAliases = aliasesToMappings[alias] ?: mutableSetOf()
            val mappingForIndex = mappings[index]
            if (mappingForIndex != null) {
                matchingAliases.add(mappingForIndex)
            }
            aliasesToMappings[alias] = matchingAliases
            requireNotNull(indicesToAliases[index]) {
                "Index is missing from map."
            }.add(alias)
        }
    }

    val noAliasMappings = mutableSetOf<Es2Mapping>()
    indicesToAliases.filter { (_, aliases) ->
        aliases.isEmpty()
    }.forEach { (index, _) ->
        mappings[index]?.let { es2Mapping -> noAliasMappings.add(es2Mapping) }
    }
    if (noAliasMappings.isNotEmpty()) {
        aliasesToMappings["null"] = noAliasMappings
    }

    return Es2Schema(indicesToAliases, indicesToSettings, indicesToStats, aliasesToMappings, mappings)

}
