package io.em2m.search.migrate

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es.models.EsVersion
import io.em2m.search.es2.operations.es2LoadSchema
import io.em2m.search.migrate.models.EsMigrationConfig
import io.em2m.search.migrate.models.EsMigrationMappingItem
import io.em2m.search.migrate.models.EsMigrationMappingObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import java.awt.Desktop
import java.io.File
import kotlin.test.assertNotNull

class EsMigrationMappingFileTest {

    var inputDirectory: File = assertNotNull(System.getenv()["esIn"]?.let { path -> File(path) },
        "No env variable passed for \"esIn\". Please check your run configuration in IntelliJ.")
    var es2Directory: File = File(inputDirectory, "es2")

    var outputFile: File = assertNotNull(System.getenv()["migrationOut"]?.let { path -> File(path) },
        "No env variable passed for \"migrationOut\". Please check your run configuration in IntelliJ.")

    init {
        outputFile.parentFile.mkdirs()
    }

    val objectMapper = jacksonObjectMapper()
    val jsonMapper = objectMapper
    val yamlMapper = ObjectMapper(YAMLFactory()
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .disable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS))

    @BeforeEach
    fun `verify directories`() {
        assert(inputDirectory.isDirectory && inputDirectory.exists()) {
            "Input directory \"${inputDirectory.absolutePath}\" doesn't exist."
        }
        assert(es2Directory.isDirectory && es2Directory.exists()) {
            "Es2 directory \"${es2Directory.absolutePath}\" doesn't exist."
        }
    }

    @Test
    fun `write migration mapping to json from file`() {
        val es2Schema = es2LoadSchema(es2Directory, objectMapper)
        val indexNames = es2Schema.indicesToMappings.keys
        val indicesToAliases = es2Schema.indicesToAliases

        val aliasMigrationMap = mutableMapOf<String, EsMigrationMappingItem>()
        val indexMigrationMap = mutableMapOf<String, EsMigrationMappingItem>()
        indexNames.forEach { es2Index ->
            val aliases = indicesToAliases[es2Index] ?: emptyList()
            val hasAlias = aliases.isNotEmpty()
            if (hasAlias) {
                aliases.forEach { alias ->
                    aliasMigrationMap[alias] = EsMigrationMappingItem(
                        mapOf(EsVersion.DEFAULT to EsMigrationConfig.DEFAULT)
                    )
                }
            } else {
                indexMigrationMap[es2Index] = EsMigrationMappingItem(
                    mapOf(EsVersion.DEFAULT to EsMigrationConfig.DEFAULT)
                )
            }
        }
        val indices = indexMigrationMap.entries.toList()
        val aliases = aliasMigrationMap.entries.toList()

        val mappingObject = EsMigrationMappingObject(indices, aliases)
        val parentDirectory = outputFile.parentFile
        val jsonFile = File(parentDirectory, "${outputFile.nameWithoutExtension}.json")
        val yamlFile = File(parentDirectory, "${outputFile.nameWithoutExtension}.yaml")

        val jsonString = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappingObject)
        val yamlString = yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mappingObject)
        jsonFile.writeText(jsonString)
        yamlFile.writeText(yamlString)

        Desktop.getDesktop().open(parentDirectory)
    }

}
