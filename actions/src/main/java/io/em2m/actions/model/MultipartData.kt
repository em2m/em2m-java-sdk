package io.em2m.actions.model

import java.io.InputStream

class MultipartData(val files: Map<String, File>, val form: Map<String, String>) {

    class File(val filename: String, val headers: Map<String, List<String>>, val inputStream: InputStream)
}
