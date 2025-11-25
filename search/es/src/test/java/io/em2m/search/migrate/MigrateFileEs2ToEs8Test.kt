package io.em2m.search.migrate

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.MAPPING_FILE_NAME
import io.em2m.search.es2.models.Es2MappingProperty
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.nio.file.Files
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.test.Test
import kotlin.test.assertNotNull

class MigrateFileEs2ToEs8Test {

    var inputFile: File = File(assertNotNull(System.getenv("es2File")))
    val objectMapper = jacksonObjectMapper()

    var outputDirectory: File = File((System.getenv()["esOut"] ?:
        Files.createTempDirectory("esOut").toFile().absolutePath))

    var es8Directory: File = File(outputDirectory, "es8")

    val prettyWriter = objectMapper.writerWithDefaultPrettyPrinter()

    @BeforeEach
    fun `verify directories`() {
        assert(inputFile.isFile && inputFile.exists()) {
            "Input file ${inputFile.absolutePath} doesn't exist or is not a file."
        }
        es8Directory.mkdirs()
        assert(outputDirectory.isDirectory) {
            "Output directory ${outputDirectory.absolutePath} doesn't exist."
        }
    }

    @Test
    fun `migrate es2 to es8 file on disk`() {
        // basically the same as MigrateDirectoryEs2ToEs8Test

        val es2MappingProperties = Es2MappingProperty.load(inputFile, objectMapper)
        val es8MappingProperties = es2MappingProperties.map { (key, es2MappingProperty) ->
            val indexName = key.parentFile.name
            val outFileParent = File(es8Directory, indexName)
            outFileParent.mkdirs()
            val es8MappingProperty = migrateEs2ToEs8(es2MappingProperty)

            File(outFileParent, "$MAPPING_FILE_NAME.json") to es8MappingProperty
        }

        es8MappingProperties.forEach { (outFile, es8MappingProperty) ->
            outFile.writeText(prettyWriter.writeValueAsString(es8MappingProperty), Charsets.UTF_8)
        }
    }

}
