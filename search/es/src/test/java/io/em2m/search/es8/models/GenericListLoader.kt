package io.em2m.search.es8.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

data class GenericListLoader<T>(val fileName: String, val clazz: Class<T>) {

    fun load(objectMapper: ObjectMapper = jacksonObjectMapper()): List<T> {
        val es8Url = GenericListLoader::class.java.classLoader.getResource("es8")
        requireNotNull(es8Url) { "es8 directory not defined in resources" }
        val es8Dir = File(es8Url.file)
        if (!es8Dir.isDirectory || !es8Dir.exists()) throw IllegalStateException("es8 needs to be a directory.")
        val reference = File(es8Dir, fileName)
        return objectMapper.readValue(reference, List::class.java).mapNotNull {
            try {
                objectMapper.convertValue(it, clazz)
            } catch (_: Exception) {
                null
            }
        }
    }

}
