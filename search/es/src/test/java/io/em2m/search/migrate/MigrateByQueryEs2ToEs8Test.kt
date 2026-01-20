package io.em2m.search.migrate

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import feign.Feign
import feign.auth.BasicAuthRequestInterceptor
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.es.*
import io.em2m.search.es8.*
import io.em2m.search.es2.Es2Api
import io.em2m.search.es2.models.Es2MappingProperty
import io.em2m.search.es2.operations.exportSchema
import io.em2m.search.es8.Es8Api
import io.em2m.search.es8.models.Es8Dynamic
import io.em2m.search.es8.models.Es8Settings
import io.em2m.utils.coerce
import org.junit.platform.commons.logging.LoggerFactory
import kotlin.test.Test

class MigrateByQueryEs2ToEs8Test {

    val logger = LoggerFactory.getLogger(MigrateByQueryEs2ToEs8Test::class.java)

    val indexWhitelist: List<String> = run {
        System.getenv()["indexWhitelist"]?.split(",") ?: emptyList()
    }.map(String::trim)

    val maxEntities: Long = run {
        System.getenv()["maxEntities"]?.toLongOrNull() ?: 10_000L
    }

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

    val es2Status = es2Client.getStatus().run {
        assert(version.major == 2) { "Unexpected source version: $version" }
    }

    val es8Url: String = System.getenv()["es8Url"] ?: "http://localhost:9200"

    val es8User = requireNotNull(System.getenv()["es8User"]) { "es8User was not set in env variables." }
    val es8Pass = requireNotNull(System.getenv()["es8Pass"]) { "es8Pass was not set in env variables." }

    val es8Client: Es8Api = Feign.builder()
        .encoder(TextPlainEncoder(JacksonEncoder(mapper)))
        .decoder(JacksonDecoder(mapper))
        .logger(Slf4jLogger())
        .requestInterceptor(BasicAuthRequestInterceptor(es8User, es8Pass))
        .logLevel(feign.Logger.Level.FULL)
        .target(Es8Api::class.java, es8Url)

    val es8Status = es8Client.getStatus().run {
        assert(version.major == 8) { "Unexpected destination version: $version" }
    }

    @Test
    fun `migrate es2 to es8 by query`() {
        // TODO: convert this to possibly a regex? this is just for testing
        val ignorePatterns = listOf<(String) -> Boolean>(
            { it.startsWith(".") },
            { it.startsWith("http") },
            { it == ".kibana" }
        )
        val es2Schema = es2Client.exportSchema(indexWhitelist=indexWhitelist)
        val mappingErrors = mutableListOf<MappingError>()
        val indexErrors = mutableListOf<IndexError>()
        val unprocessedIndices = es2Schema.indicesToMappings.keys.toMutableSet()
        es2Schema.aliasesToMappings.forEach { (alias, mappings) ->
            if (mappings.size > 1 && alias != "null") {
                logger.warn{ "TODO: ($alias : ${mappings.indices.firstOrNull()})" }
                return@forEach
            }
            val es2Mapping = mappings.firstOrNull() ?: run {
                logger.warn { "Empty mappings for alias: $alias" }
                return@forEach
            }
            val type = es2Mapping.type
            val index = es2Mapping.index
            if (ignorePatterns.any { it(index) }) return@forEach
            val es2MappingProperty = es2Mapping.properties.coerce<Es2MappingProperty>() ?: run {
                logger.warn { "Coerce error on index: $index" }
                return@forEach
            }
            val settings = Es8Settings().toObjectNode()
            val es8MappingProperty = migrateEs2ToEs8(es2MappingProperty)
            try {
                val indexExists = try {
                    es8Client.indexExists(index)
                    true
                } catch (_ : Exception) {
                    false
                }

                if (indexExists && ("localhost:9200" in es8Url || "dev" in es8Url)) {
                    es8Client.deleteIndex(index)
                } else if (indexExists){
                    logger.warn { "Can't delete from a non localhost remote for safety reasons." }
                    return@forEach
                }

                es8Client.createIndex(index, settings)
                es8Client.flush()
                Thread.sleep(500L)

                val entities: List<Pair<String, ObjectNode>> = es2Client.search(index, type, EsSearchRequest(
                    from= 0,
                    size = maxEntities
                )).hits.hits.mapNotNull { hit ->
                    val source = hit.source ?: return@mapNotNull null
                    hit.id to source
                }

                es8MappingProperty.dynamic = Es8Dynamic.FALSE
                es8Client.putMapping(index, es8MappingProperty, mapper)

                entities.forEach { (id, entity) ->
                    try {
                        es8Client.put(index, id, entity)
                    } catch (ex: Exception) {
                        logger.error(ex) { "Error putting entity." }
                        indexErrors.add(IndexError(
                            index=index,
                            oldMapping = es2Mapping,
                            newMapping = es8MappingProperty,
                            entity = entity,
                            exception = ex
                        ))
                    }
                }

                if (alias == "null") {
                    logger.warn { "Alias was null for index: $index" }
                    return@forEach
                }
                val aliasRequest = EsAliasRequest()
                aliasRequest.actions.add(EsAliasAction(add = EsAliasDefinition(index, alias)))

                es8Client.putAliases(aliasRequest)
            } catch (ex: Exception) {
                mappingErrors.add(MappingError(
                    index=index,
                    oldMapping = es2Mapping,
                    newMapping = es8MappingProperty,
                    exception= ex
                ))
            } finally {
                unprocessedIndices.remove(index)
            }
        }

        unprocessedIndices.forEach { index ->
            if (ignorePatterns.any { it(index) }) return@forEach

            val es2Mapping = es2Schema.indicesToMappings[index] ?: return@forEach
            val type = es2Mapping.type
            val es8Mapping = migrateEs2ToEs8(es2Mapping)
            val es8MappingProperty = es8Mapping.properties
            val settings = Es8Settings().toObjectNode()

            try {
                val indexExists = try {
                    es8Client.indexExists(index)
                    true
                } catch (_ : Exception) {
                    false
                }

                if (indexExists && ("localhost:9200" in es8Url || "dev" in es8Url)) {
                    es8Client.deleteIndex(index)
                } else if (indexExists){
                    logger.warn { "Can't delete index from a non localhost or dev remote for safety reasons." }
                    return@forEach
                }

                es8Client.createIndex(index, settings)
                es8Client.flush()
                Thread.sleep(500L)

                val entities: List<Pair<String, ObjectNode>> = es2Client.search(index, type, EsSearchRequest(
                    from= 0,
                    size = maxEntities
                )).hits.hits.mapNotNull { hit ->
                    val source = hit.source ?: return@mapNotNull null
                    hit.id to source
                }

                es8MappingProperty.dynamic = Es8Dynamic.FALSE
                es8Client.putMapping(index, es8MappingProperty, mapper)

                entities.forEach { (id, entity) ->
                    try {
                        es8Client.put(index, id, entity)
                    } catch (ex: Exception) {
                        logger.error(ex) { "Error putting entity." }
                        indexErrors.add(IndexError(
                            index=index,
                            oldMapping = es2Mapping,
                            newMapping = es8MappingProperty,
                            entity = entity,
                            exception = ex
                        ))
                    }
                }
            } catch (ex: Exception) {
                mappingErrors.add(MappingError(
                    index=index,
                    oldMapping = es2Mapping,
                    newMapping = es8MappingProperty,
                    exception= ex
                ))
            } finally {
            }
        }

        println("Index Errors: ${indexErrors.size}")
        println("Mapping Errors: ${mappingErrors.size}")


    }
}
