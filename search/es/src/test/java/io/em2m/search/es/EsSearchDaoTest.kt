package io.em2m.search.es

import io.em2m.geo.feature.Feature
import io.em2m.search.core.model.*
import io.em2m.search.core.model.Direction.Ascending
import org.junit.Before
import org.junit.Test
import rx.observers.TestSubscriber
import kotlin.properties.Delegates

class EsSearchDaoTest : FeatureTestBase() {

    private var searchDao: SearchDao<Feature> by Delegates.notNull()

    @Before
    override fun before() {
        super.before()
        searchDao = EsSearchDao(esClient, FeatureTestBase.index, FeatureTestBase.type,  Feature::class.java, idMapper, es6 = FeatureTestBase.es6)
    }

    @Test
    fun testCreate() {
        val sub = TestSubscriber<Any>()
        val f = Feature()
        val name = "TEST_CREATE_FEATURE"
        f.properties["name"] = name

        searchDao.create(f).doOnNext { created ->
            assertNotNull("Id should not be null", created.id)
        }.flatMap { created ->
            esClient.flush()
            searchDao.findById(created.id!!)
        }.doOnNext {
            assertEquals(name, it?.properties?.get("name"))
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
        sub.assertCompleted()
    }

    @Test
    fun testDeleteByKey() {
        val sub = TestSubscriber<Any>()
        val key = "nn00456574"
        searchDao.deleteById(key).doOnNext {
            esClient.flush()
        }.flatMap {
            searchDao.exists(key)
        }.doOnNext {
            assertFalse("Record should not exist", it)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testExists() {
        searchDao.exists("nn00456574").doOnNext(::assertTrue).toBlocking().first()
        searchDao.exists("_invalid_id").doOnNext(::assertFalse).toBlocking().first()
    }

    @Test
    fun testCount() {
        val sub = TestSubscriber<Any>()
        searchDao.count(MatchAllQuery()).doOnNext { count ->
            assertEquals(46, count)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testFindByKey() {
        searchDao.findById("nn00456574").doOnNext(::assertNotNull).toBlocking().first()
    }

    @Test
    fun testFindOne() {
        val sub = TestSubscriber<Any>()
        searchDao.findOne(MatchAllQuery()).doOnNext { first ->
            assertTrue(first is Feature)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testSave() {
        val sub = TestSubscriber<Any>()
        val f = Feature()
        f.id = "SAVED_FEATURE"
        searchDao.save(f.id!!, f).flatMap {
            esClient.flush()
            searchDao.findById(f.id!!)
        }.doOnNext { feature ->
            assertEquals("Id should match", f.id, feature?.id)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testSaveBatch() {
        val sub = TestSubscriber<Any>()
        val f1 = Feature()
        val f2 = Feature()
        val f3 = Feature()
        f1.id = "1"
        f2.id = "2"
        f3.id = "3"
        searchDao.saveBatch(listOf(f1, f2, f3))
                .doOnNext { esClient.flush() }
                .flatMap { searchDao.exists("1") }.doOnNext(::assertTrue)
                .flatMap { searchDao.exists("2") }.doOnNext(::assertTrue)
                .flatMap { searchDao.exists("3") }.doOnNext(::assertTrue)
                .subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testMatchAll() {
        val request = SearchRequest(offset = 0, limit = 20, query = MatchAllQuery())
        val sub = TestSubscriber<Any>()
        searchDao.search(request).doOnNext { result ->
            assertEquals(46, result.totalItems)
            assertEquals(20, result.items?.size)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testScrollItems() {
        val request = SearchRequest(limit = 5, query = MatchAllQuery(), params = mapOf("scroll" to "1m"))
        val sub = TestSubscriber<Any>()
        val esDao = searchDao as EsSearchDao
        esDao.search(request).flatMap { result -> esDao.scrollItems(request, result) }
                .doOnNext { result ->
                    assertTrue(result != null)
                }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
        assertEquals(46, sub.valueCount)
    }

    @Test
    fun testScrollRows() {
        val request = SearchRequest(limit = 5, fields = listOf(Field(name = "id")), query = MatchAllQuery(), params = mapOf("scroll" to "1m"))
        val sub = TestSubscriber<Any>()
        val esDao = searchDao as EsSearchDao
        esDao.search(request).flatMap { result -> esDao.scrollRows(request, result) }
                .doOnNext { result ->
                    assertTrue(result != null)
                }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
        assertEquals(46, sub.valueCount)
    }


    @Test
    fun testSort() {
        val sub = TestSubscriber<Any>()
        val request = SearchRequest(0, 10, MatchAllQuery(), sorts = mutableListOf(DocSort("properties.mag", Ascending)))
        searchDao.search(request)
                .doOnNext { result ->
                    val mag = result.items?.getOrNull(0)?.properties?.get("mag") as Double
                    assertEquals(2.5, mag, 0.0001)
                }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

    @Test
    fun testFields() {
        val request = SearchRequest(offset = 0, limit = 20, fields = listOf(Field("id"), Field("properties.mag")), query = MatchAllQuery())
        val sub = TestSubscriber<Any>()
        searchDao.search(request).doOnNext { result ->
            assertEquals(listOf(Field("id"), Field("properties.mag")), result.fields)
            assertEquals(20, result.rows?.size)
            assertNull(result.items)
            assertTrue(result.rows?.get(0)?.get(0) is String)
            assertTrue(result.rows?.get(0)?.get(1) is Double)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
    }

}
