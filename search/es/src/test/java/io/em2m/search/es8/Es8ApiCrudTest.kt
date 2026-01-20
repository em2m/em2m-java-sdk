package io.em2m.search.es8

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es8.models.FoodCategory
import io.em2m.search.es8.operations.es8GenerateMappingProperties
import feign.Feign
import feign.auth.BasicAuthRequestInterceptor
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.es.TextPlainEncoder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.Test
import kotlin.test.assertFalse

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Es8ApiCrudTest {

    val es8Url: String = System.getenv()["es8Url"] ?: "http://localhost:9200"

    val es8User = requireNotNull(System.getenv()["es8User"]) { "es8User was not set in env variables." }
    val es8Pass = requireNotNull(System.getenv()["es8Pass"]) { "es8Pass was not set in env variables." }

    val mapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val es8Client: Es8Api = Feign.builder()
        .encoder(TextPlainEncoder(JacksonEncoder(mapper)))
        .decoder(JacksonDecoder(mapper))
        .logger(Slf4jLogger())
        .requestInterceptor(BasicAuthRequestInterceptor(es8User, es8Pass))
        .logLevel(feign.Logger.Level.FULL)
        .target(Es8Api::class.java, es8Url)

    companion object {
        // These need to be in here so it's statically shared between tests
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
        private val dateString = dateFormatter.format(Date())

        private const val INDEX_NAME_PREFIX = "es8-api-test-food-category"
        private val INDEX_NAME = "$INDEX_NAME_PREFIX-${dateString}"
        private const val INDEX_ALIAS = "es8-food-category"

        // This is only thread-safe because we can specify the execution order of the tests
        private val testScope = mutableMapOf<String, Any>()
    }

    @BeforeEach
    fun flush() {
        es8Client.flush()
    }

    @Test
    @Order(0)
    fun `get status`() {
        val status = es8Client.getStatus()
        assert(status.version.major == 8) { "This test class only supports es8" }
    }

    @Test
    @Order(10)
    fun `create index`() {
        es8Client.createIndex(INDEX_NAME)
    }

    @Test
    @Order(30)
    fun `put mapping`() {
        val categorySchema = es8GenerateMappingProperties(FoodCategory::class.java)
        es8Client.putMapping(INDEX_NAME, categorySchema)
        println(categorySchema)
    }

    @Test
    @Order(40)
    fun `get mapping`() {
        val mapping = es8Client.getMapping(INDEX_NAME)
        println(mapping)
    }

    @Test
    @Order(50)
    fun `add alias`() {
        es8Client.addAlias(INDEX_NAME, INDEX_ALIAS)
    }

    @Test
    @Order(60)
    fun `get aliases`() {
        val indicesToAliases = es8Client.getIndicesToAliases()
        println(indicesToAliases)
        assert(indicesToAliases[INDEX_NAME]?.contains(INDEX_ALIAS) == true )
    }

    @Test
    @Order(70)
    fun `get stats`() {
        val stats = es8Client.getStats(INDEX_NAME)
        println(stats)
    }

    @Test
    @Order(80)
    fun `put with id`() {
        val dummyData = FoodCategory.load().random()
        val id = UUID.randomUUID().toString()
        es8Client.put(INDEX_NAME, id, dummyData)
        testScope["putId"] = id
    }

    @Test
    @Order(90)
    fun `exists by id`() {
        val putId: String by testScope
        val exists = es8Client.exists(INDEX_NAME, putId)
        assert(exists)
    }

    @Test
    @Order(100)
    fun `delete by id`() {
        val putId: String by testScope
        es8Client.delete(INDEX_NAME, putId)
    }

    @Test
    @Order(110)
    fun `not exist by id`() {
        val putId: String by testScope
        val exists = es8Client.exists(INDEX_NAME, putId)
        assertFalse(exists)
    }

    @Test
    @Order(120)
    fun `remove alias`() {
        es8Client.removeAlias(INDEX_NAME, INDEX_ALIAS)
    }

    @Test
    @Order(130)
    fun `get alias is removed`() {
        val indicesToAliases = es8Client.getIndicesToAliases()
        println(indicesToAliases)
        assert(indicesToAliases[INDEX_NAME]?.contains(INDEX_ALIAS) == false )
    }

    @Test
    @Order(1000)
    fun `delete index`() {
        es8Client.deleteIndex(INDEX_NAME)
    }

}
