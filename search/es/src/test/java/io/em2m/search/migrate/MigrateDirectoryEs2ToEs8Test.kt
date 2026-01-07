package io.em2m.search.migrate

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.MAPPING_FILE_NAME
import io.em2m.search.es2.models.Es2MappingProperty
import org.junit.jupiter.api.BeforeEach
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull

class MigrateDirectoryEs2ToEs8Test {

    var inputDirectory: File = assertNotNull(System.getenv()["esIn"]?.let { path -> File(path) },
        "No env variable passed for \"esIn\". Please check your run configuration for environment variables.")
    var es2Directory: File = File(inputDirectory, "es2")

    val objectMapper = jacksonObjectMapper()

    val indexWhitelist: List<String> = run {
        System.getenv()["indexWhitelist"]?.split(",") ?: emptyList()
    }.map(String::trim)

    var outputDirectory: File = File((System.getenv()["esOut"] ?: inputDirectory.absolutePath))

    var es8Directory: File = File(outputDirectory, "es8")

    val prettyWriter = objectMapper.writerWithDefaultPrettyPrinter()

    @BeforeEach
    fun `verify directories`() {
        assert(inputDirectory.isDirectory && inputDirectory.exists()) {
            "Input directory \"${inputDirectory.absolutePath}\" doesn't exist."
        }
        assert(es2Directory.isDirectory && es2Directory.exists()) {
            "Es2 directory \"${es2Directory.absolutePath}\" doesn't exist."
        }
        es8Directory.mkdirs()
    }

    @Test
    fun `migrate es2 to es8 on disk`() {
        val es2MappingProperties = Es2MappingProperty.load(es2Directory, objectMapper, indexWhitelist)
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
