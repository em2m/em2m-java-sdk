package io.em2m.search.es8

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es8.models.Movie
import io.em2m.search.es8.operations.es8GenerateMappingProperties
import feign.Feign
import feign.auth.BasicAuthRequestInterceptor
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.es.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.Test

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Es8ApiSearchTest {

    val es8Url: String = System.getenv()["es8Url"] ?: "http://localhost:9200"

    val es8User = requireNotNull(System.getenv()["es8User"]) { "es8User was not set in env variables." }
    val es8Pass = requireNotNull(System.getenv()["es8Pass"]) { "es8Pass was not set in env variables." }

    val mapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)

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

        private const val INDEX_NAME_PREFIX = "es8-api-test-movies"
        private val INDEX_NAME = "$INDEX_NAME_PREFIX-${dateString}"
        private const val INDEX_ALIAS = "es8-movie"

        // This is only thread-safe because we can specify the execution order of the tests
        private val testScope = mutableMapOf<String, Any>()

        private var dummyData = mutableListOf<Movie>()
    }

    @BeforeEach
    fun flush() {
        es8Client.flush()
    }

    @Test
    @Order(0)
    fun `load data from disk`() {
        dummyData.clear()
        dummyData.addAll(Movie.load(mapper))
        assert(dummyData.isNotEmpty())
    }

    @Test
    @Order(10)
    fun `create index`() {
        es8Client.createIndex(INDEX_NAME)
    }

    @Test
    @Order(20)
    fun `put mapping`() {
        val movieSchema = es8GenerateMappingProperties(dummyData.random())
        es8Client.putMapping(INDEX_NAME, movieSchema)
    }

    @Test
    @Order(30)
    fun `add alias`() {
        es8Client.addAlias(INDEX_NAME, INDEX_ALIAS)
    }

    private fun bulkIndex(index: String, id: String, entity: Movie): String {
        val line1 = """{ "index": {"_index": "$index", "_id": "$id"} }"""
        val line2 = mapper.disable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(entity)
        return "$line1\n$line2\n"
    }

    @Test
    @Order(40)
    fun `bulk write`() {
        // TODO: Create helper function for this
        val bulkOperations = dummyData.joinToString("") {
            bulkIndex(INDEX_NAME, it.id, it)
        }

        val bulkResult = es8Client.bulkUpdate(bulkOperations)
        println("Waiting for writes to be recognized.")
        Thread.sleep(2500L)
    }

    @Test
    @Order(50)
    fun `search match all movies`() {
        val searchResult = es8Client.search(INDEX_NAME)
        println(searchResult)
    }

    @Test
    @Order(55)
    fun `search comedies`() {
        val query = EsBoolQuery(must = listOf(EsTermQuery(
            "fields.genres", "Comedy"
        )))
        val searchRequest = EsSearchRequest(query = query)
        val searchResult = es8Client.search(INDEX_NAME, searchRequest)
        println(searchResult)
    }

    @Test
    @Order(57)
    fun `search good comedies`() {
        val query = EsBoolQuery(must = listOf(
            EsTermQuery("fields.genres", "Comedy"),
            EsRangeQuery("fields.rating", gte = 7.0)
        ))
        val searchRequest = EsSearchRequest(query = query)
        val searchResult = es8Client.search(INDEX_NAME, searchRequest)
        println(searchResult)
    }

    @Test
    @Order(60)
    fun `agg years`() {
        val aggs = EsAggs()
        aggs.term("fields.year","fields.year", 1000, EsSortType.COUNT, EsSortDirection.DESC)
        val searchRequest = EsSearchRequest(size = 0L, aggs = aggs)
        val searchResult = es8Client.search(INDEX_NAME, searchRequest)
        println(searchResult)
    }

    @Test
    @Order(90)
    fun `remove alias`() {
        es8Client.removeAlias(INDEX_NAME, INDEX_ALIAS)
    }

    @Test
    @Order(1000)
    fun `delete index`() {
        es8Client.deleteIndex(INDEX_NAME)
    }

}
