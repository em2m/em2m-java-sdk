package io.em2m.search.es

import io.em2m.geo.feature.Feature
import io.em2m.search.core.model.*
import io.em2m.search.core.model.Direction.Ascending
import org.junit.Before
import org.junit.Test
import kotlin.properties.Delegates

class EsSearchDaoTest : FeatureTestBase() {

    private var searchDao: SyncDao<Feature> by Delegates.notNull()

    @Before
    override fun before() {
        super.before()
        searchDao = EsSyncDao(esClient, index, type, Feature::class.java, idMapper)
    }

    @Test
    fun testCreate() {
        val f = Feature()
        val name = "TEST_CREATE_FEATURE"
        f.properties["name"] = name

        val created = searchDao.create(f)
        assertNotNull("Id should not be null", created?.id)
        esClient.flush()
        val item = searchDao.findById(created?.id!!)
        assertEquals(name, item?.properties?.get("name"))
    }

    @Test
    fun testDeleteByKey() {
        val key = "nn00456574"
        searchDao.deleteById(key)
        esClient.flush()
        assertFalse("Record should not exist", searchDao.exists(key))
    }

    @Test
    fun testExists() {
        assertTrue(searchDao.exists("nn00456574"))
        assertFalse(searchDao.exists("_invalid_id"))
    }

    @Test
    fun testCount() {
        assertEquals(46, searchDao.count(MatchAllQuery()))
    }

    @Test
    fun testFindByKey() {
        assertNotNull(searchDao.findById("nn00456574"))
    }

    @Test
    fun testFindOne() {
        val first = searchDao.findOne(MatchAllQuery())
        assertTrue(first is Feature)
    }

    @Test
    fun testSave() {
        val f = Feature()
        f.id = "SAVED_FEATURE"
        searchDao.save(f.id!!, f)
        esClient.flush()
        assertEquals(f.id, searchDao.findById(f.id!!)?.id)
    }

    @Test
    fun testSaveBatch() {
        val f1 = Feature()
        val f2 = Feature()
        val f3 = Feature()
        f1.id = "1"
        f2.id = "2"
        f3.id = "3"
        searchDao.saveBatch(listOf(f1, f2, f3))
        esClient.flush()
        assertTrue(searchDao.exists("1"))
        assertTrue(searchDao.exists("2"))
        assertTrue(searchDao.exists("3"))
    }

    @Test
    fun testMatchAll() {
        val request = SearchRequest(offset = 0, limit = 20, query = MatchAllQuery())
        val result = searchDao.search(request)
        assertEquals(46, result.totalItems)
        assertEquals(20, result.items?.size)
    }

    @Test
    fun testScrollItems() {
        val request = SearchRequest(limit = 5, query = MatchAllQuery(), params = mapOf("scroll" to "1m"))
        val esDao = searchDao as EsSyncDao
        val result = esDao.search(request)
        val scrollResult = esDao.scrollItems(request, result).firstOrNull()
        assertTrue(scrollResult != null)
    }

    @Test
    fun testScrollRows() {
        val request = SearchRequest(limit = 5, fields = listOf(Field(name = "id")), query = MatchAllQuery(), params = mapOf("scroll" to "1m"))
        val esDao = searchDao as EsSyncDao
        val result = esDao.search(request)
        val scrollResult = esDao.scrollRows(request, result)
        assertNotNull(scrollResult.firstOrNull())
    }


    @Test
    fun testSort() {
        val request = SearchRequest(0, 10, MatchAllQuery(), sorts = mutableListOf(DocSort("properties.mag", Ascending)))
        val result = searchDao.search(request)
        val mag = result.items?.getOrNull(0)?.properties?.get("mag") as Double
        assertEquals(2.5, mag, 0.0001)
    }

    @Test
    fun testFields() {
        val request = SearchRequest(offset = 0, limit = 20, fields = listOf(Field("id"), Field("properties.mag")), query = MatchAllQuery())
        val result = searchDao.search(request)
        assertEquals(listOf(Field("id"), Field("properties.mag")), result.fields)
        assertEquals(20, result.rows?.size)
        assertNull(result.items)
        assertTrue(result.rows?.get(0)?.get(0) is String)
        assertTrue(result.rows?.get(0)?.get(1) is Double)
    }

}
