package io.em2m.search.es2.operations

import io.em2m.search.MAPPINGS_KEY
import io.em2m.search.es2.Es2Api
import io.em2m.search.es2.models.Es2Mapping
import io.em2m.utils.coerce

@Deprecated("Migrate to Es8Api")
fun Es2Api.getAllMappings(): Map<String, Es2Mapping> {
    return es2GetAllMappings(this)
}

private fun es2GetAllMappings(es2Client: Es2Api): Map<String, Es2Mapping> {
    val retMap = mutableMapOf<String, Es2Mapping>()
    val indices = es2Client.getIndices()
    indices.forEach { map ->
        val mapping = es2Client.getMapping(map.index)
        var tempMap: Map<String, Any?> = mapping
        while (MAPPINGS_KEY !in tempMap) {
            // For some reason mappings have the index triply nested
            tempMap = mapping[map.index].coerce() ?: break
        }
        val allProperties = tempMap[MAPPINGS_KEY].coerce<Map<String, Map<String, Any?>>>() ?: return@forEach
        allProperties.forEach { (type, properties) ->
            retMap[map.index] = Es2Mapping(index = map.index, type = type, properties = properties)
        }
    }
    return retMap
}
