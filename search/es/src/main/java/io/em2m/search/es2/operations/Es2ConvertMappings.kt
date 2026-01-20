package io.em2m.search.es2.operations

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es2.models.Es2Mapping

fun es2ConvertMappings(input: String, objectMapper: ObjectMapper = jacksonObjectMapper()): Es2Mapping? {
    val tree = try {
        objectMapper.readValue(input, Es2Mapping::class.java)
    } catch (ex : Exception) {
        System.err.println(ex.message)
        null
    }
    return tree
}
