package io.em2m.search.migrate

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.es.TextPlainEncoder
import io.em2m.search.es.models.EsVersion
import io.em2m.search.es2.Es2Api
import io.em2m.search.es2.getAliasesMap
import io.em2m.search.migrate.models.EsMigrationConfig
import io.em2m.search.migrate.models.EsMigrationMappingItem
import io.em2m.search.migrate.models.EsMigrationMappingObject
import org.junit.jupiter.api.Test
import org.junit.platform.commons.logging.LoggerFactory
import java.awt.Desktop
import java.io.File
import kotlin.test.assertNotNull

class EsMigrationMappingSchemaTest {


    val logger = LoggerFactory.getLogger(MigrateByQueryEs2ToEs8Test::class.java)

    val indexWhitelist: List<String> = run {
        System.getenv()["indexWhitelist"]?.split(",") ?: emptyList()
    }.map(String::trim)

    var es2Url: String = System.getenv()["es2Url"] ?: "http://localhost:9200"

    val mapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val es2Client: Es2Api =  Feign.builder()
        .encoder(TextPlainEncoder(JacksonEncoder(mapper)))
        .decoder(JacksonDecoder(mapper))
        .logger(Slf4jLogger())
        .logLevel(feign.Logger.Level.FULL)
        .target(Es2Api::class.java, es2Url)

    var outputFile: File = assertNotNull(System.getenv()["migrationOut"]?.let { path -> File(path) },
        "No env variable passed for \"migrationOut\". Please check your run configuration in IntelliJ.")

    init {
        outputFile.parentFile.mkdirs()
    }

    val objectMapper = jacksonObjectMapper()
    val jsonMapper = objectMapper
    val yamlMapper = ObjectMapper(YAMLFactory()
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        .disable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
    )

    @Test
    fun `write migration mapping to json from schema`() {
        val ignorePatterns = listOf<(String) -> Boolean>(
            { it.startsWith(".") },
            { it.startsWith("http") },
            { it == ".kibana" }
        )
        val indexNames = es2Client.getIndices().mapTo(mutableSetOf()) { it.index }
        val aliasNames = es2Client.getAliasesMap()

        val indexMigrationMap = mutableMapOf<String, EsMigrationMappingItem>()
        val aliasMigrationMap = mutableMapOf<String, EsMigrationMappingItem>()
        indexNames.forEach { es2Index ->
            if (indexWhitelist.isNotEmpty() && es2Index !in indexWhitelist) return@forEach
            if (ignorePatterns.any { it(es2Index)} ) return@forEach
            val aliases = aliasNames[es2Index] ?: emptyList()
            val hasAlias = aliases.isNotEmpty()
            if (hasAlias) {
                aliases.forEach { alias ->
                    aliasMigrationMap[alias] = EsMigrationMappingItem (
                        mapOf(EsVersion.DEFAULT to EsMigrationConfig.DEFAULT)
                    )
                }
            } else {
                indexMigrationMap[es2Index] = EsMigrationMappingItem (
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
