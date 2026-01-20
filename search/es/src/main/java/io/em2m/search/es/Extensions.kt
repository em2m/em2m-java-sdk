package io.em2m.search.es


fun EsApi.getIndicesToAliases(): Map<String, List<String>> {
    val allAliases = getAliases()
    return allAliases.fieldNames().asSequence()
        .filterNot { it.startsWith(".") } // es-defined hidden aliases
        .associateWith { indexName ->
            allAliases.get(indexName).get("aliases").fieldNames()
                .asSequence()
                .toList()
        }
}

fun EsApi.getAliases(index: String): List<String> {
    val indicesToAliases = getIndicesToAliases()
    return indicesToAliases[index] ?: emptyList()
}
