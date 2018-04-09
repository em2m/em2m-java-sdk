package io.em2m.actions.model

import org.slf4j.LoggerFactory
import java.io.InputStream
import javax.servlet.http.Part

data class MultipartData(val files: Map<String, File>, val form: Map<String, String>) {

    class File(val filename: String, val headers: Map<String, List<String>>, val inputStream: InputStream)

    companion object {

        val log = LoggerFactory.getLogger(javaClass)

        fun fromParts(parts: List<Part>): MultipartData {

            val files = HashMap<String, File>()
            val form = HashMap<String, String>()
            parts.forEach { part ->
                val filename = extractFileName(part)
                if (filename?.isNotEmpty() == true) {
                    val headers = HashMap<String, List<String>>()
                    files.put(part.name, File(filename, headers, part.inputStream))
                } else {
                    try {
                        val name = part.name
                        val text = part.inputStream.reader().readText()
                        form.put(name, text)
                    } catch (ex: Exception) {
                        log.error("Error parsing part ${part.name}", ex)
                    }
                }
            }
            return MultipartData(files, form)
        }

        fun extractFileName(part: Part): String? {
            val contentDisp = part.getHeader("content-disposition");
            val items = contentDisp.split(";");
            for (s in items) {
                if (s.trim().startsWith("filename")) {
                    return s.substring(s.indexOf("=") + 2, s.length - 1);
                }
            }
            return null;
        }

    }
}
