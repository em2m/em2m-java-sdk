package io.em2m.search.es8

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es8.dao.Es8SyncDao
import io.em2m.search.es8.models.Movie
import io.em2m.search.es8.models.MovieIdMapper
import feign.Feign
import feign.auth.BasicAuthRequestInterceptor
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.core.model.MatchAllQuery
import io.em2m.search.core.model.SearchRequest
import io.em2m.search.es.TextPlainEncoder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.Test

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Es8SyncDaoTest {

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

    val es8SyncDao = Es8SyncDao<Movie>(
        es8Client,
        INDEX_NAME,
        Movie::class.java,
        MovieIdMapper())


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
    @Order(30)
    fun `test write all`() {
        es8SyncDao.saveBatch(dummyData)
        Thread.sleep(2500L)
    }

    @Test
    @Order(50)
    fun `test search all`() {
        val result = es8SyncDao.search(SearchRequest(query = MatchAllQuery()))
        assert(result.totalItems == dummyData.size.toLong())
    }


    @Test
    @Order(55)
    fun `test get 55`() {
        val result = es8SyncDao.search(SearchRequest(limit = 55, query = MatchAllQuery()))
        assert(result.items?.size == 55)
    }

    @Test
    @Order(1000)
    fun `delete index`() {
        es8Client.deleteIndex(INDEX_NAME)
    }


}
