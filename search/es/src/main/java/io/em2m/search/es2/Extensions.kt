package io.em2m.search.es2

fun Es2Api.getAliasesMap(): Map<String, List<String>> {
    return getAliases().fields().asSequence().associate { (index, fields) ->
        index to fields.get("aliases").fieldNames().asSequence().toList()
    }
}
