package io.em2m.actions.model

import org.slf4j.LoggerFactory
import java.io.InputStream

data class MultipartData(val files: Map<String, File>, val form: Map<String, String>) {

    class File(val filename: String, val headers: Map<String, List<String>>, val inputStream: InputStream)

    companion object {

        val log = LoggerFactory.getLogger(MultipartData::class.java)

    }
}
