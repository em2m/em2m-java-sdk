/**
 * ELASTIC M2M Inc. CONFIDENTIAL
 * __________________

 * Copyright (c) 2013-2016 Elastic M2M Incorporated, All Rights Reserved.

 * NOTICE:  All information contained herein is, and remains
 * the property of Elastic M2M Incorporated

 * The intellectual and technical concepts contained
 * herein are proprietary to Elastic M2M Incorporated
 * and may be covered by U.S. and Foreign Patents,  patents in
 * process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elastic M2M Incorporated.
 */
package io.em2m.search.mongo

import com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
import com.fasterxml.jackson.databind.node.ObjectNode
import com.mongodb.ConnectionString
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.connection.ClusterSettings
import com.mongodb.rx.client.MongoClients
import com.typesafe.config.ConfigFactory
import io.em2m.search.core.model.*
import io.em2m.search.core.model.Direction.Descending
import io.em2m.search.core.parser.SimpleSchemaMapper
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rx.observers.TestSubscriber
import kotlin.properties.Delegates

class MongoSearchDaoTest : Assert() {

    private var dao: MongoSearchDao<ObjectNode> by Delegates.notNull()
    private var schemaMapper: SimpleSchemaMapper by Delegates.notNull()
    val config = ConfigFactory.load()

    @Before
    @Throws(Exception::class)
    fun before() {
        schemaMapper = SimpleSchemaMapper("test")
        schemaMapper.withMapping("number", Int::class.java)
        schemaMapper.withMapping("_id", String::class.java)

        // Mongo Database
        val mongoUri = config.getString("mongo.uri")
        val mongoDb = config.getString("mongo.db")
        val settings = MongoClientSettings.builder()
                .clusterSettings(ClusterSettings.builder().applyConnectionString(ConnectionString(mongoUri)).build())
                .build()
        val client = MongoClients.create(settings)
        val database = client.getDatabase((mongoDb))

        val collection = database.getCollection("test")

        val documentMapper = JacksonDocumentMapper(ObjectNode::class.java)
        dao = MongoSearchDao(TestIdMapper(), documentMapper, collection, schemaMapper)
        dao.dropCollection().toBlocking().first()
    }

    @After
    @Throws(Exception::class)
    fun after() {
        dao.close()
    }

    @Test
    fun testSave() {
        val item = instance.objectNode()
        item.put("value", "value").put("number", 1)
        val result = dao.create(item).doOnNext { result -> println("created:" + result) }.toBlocking().first()
        val item2 = dao.findById(result.get("_id").asText()).toBlocking().first()
        assertNotNull(item2)
    }

    @Test
    fun testOnSubscribe() {
        val item = instance.objectNode()
        item.put("value", "value").put("number", 1)
        dao.create(item)
                .doOnSubscribe { println("started") }
                .doOnError { println("error") }
                .doOnCompleted { println("completed") }
                .doOnNext { result -> println("created:" + result) }
                .toList().toBlocking().first()
    }

    @Test
    @Throws(Exception::class)
    fun testUpdate() {
        val item = instance.objectNode()
        item.put("value", "value").put("number", 1)
        val single = dao.create(item).doOnNext { result -> println("created:" + result) }.toBlocking().single()
        val id = single.get("_id").asText()
        item.put("value", "value-2").put("number", 2)
        dao.save(id, item).doOnNext { result -> println("updated:" + result) }.toBlocking().single()
        val item2 = dao.findById(id).toBlocking().first()
        assertNotNull(item2)
        assertEquals(2, item2?.get("number")?.asInt())
    }

    @Test
    @Throws(Exception::class)
    fun testSearch() {
        for (i in 0..9) {
            val item = instance.objectNode()
            item.put("value", "_" + i).put("number", i)
            dao.create(item).toBlocking().first()
        }
        val request = SearchRequest(offset = 2, limit = 5,
                sorts = listOf(DocSort("number", Descending)),
                query = RangeQuery("number", gte = 1, lte = 7)
        )

        val results = dao.search(request).toBlocking().first()
        assertNotNull(results)

        // 7 items match (7,6,5,4,3,2,1)
        assertEquals(7, results.totalItems)
        // 5 items returned (5,4,3,2,1)
        assertEquals(5, results.items?.size)
        // 5 is first since offset = 2
        assertEquals(5, results.items?.get(0)?.get("number")?.asInt())
    }

    @Test
    fun testStreamRows() {
        for (i in 0..999) {
            val item = instance.objectNode()
            item.put("value", "_" + i).put("number", i)
            dao.create(item).toBlocking().first()
        }
        val request = SearchRequest(limit = Int.MAX_VALUE.toLong(), fields = listOf(Field("value")), sorts = listOf(DocSort("number", Descending)))
        val sub = TestSubscriber<Any>()
        dao.streamRows(request).doOnNext {
            assertNotNull(it)
            assertTrue(it is List)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
        assertEquals(1000, sub.valueCount)
    }

    @Test
    fun testStreamItems() {
        for (i in 0..999) {
            val item = instance.objectNode()
            item.put("value", "_" + i).put("number", i)
            dao.create(item).toBlocking().first()
        }
        val request = SearchRequest(limit = Int.MAX_VALUE.toLong(), sorts = listOf(DocSort("number", Descending)))
        val sub = TestSubscriber<Any>()
        dao.streamItems(request).doOnEach {
            assertNotNull(it)
        }.subscribe(sub)
        sub.awaitTerminalEvent()
        sub.assertNoErrors()
        assertEquals(1000, sub.valueCount)
    }


    @Test
    @Throws(Exception::class)
    fun testFilters() {
        for (i in 0..9) {
            val item = instance.objectNode()
            item.put("value", "_$i").put("number", i)
            dao.create(item).toBlocking().first()
        }
        Thread.sleep(1000)

        val request = SearchRequest(
                query = AndQuery(of = listOf(
                        OrQuery(of = listOf(TermQuery("number", 5), TermQuery("number", 6))),
                        RangeQuery("number", gte = 5, lte = 9),
                        RangeQuery("value", gte = "_5", lte = "_9"))
                )
        )

        val results = dao.search(request).toBlocking().first()
        assertNotNull(results)
        assertEquals(2, results.totalItems.toInt().toLong())
    }

    class TestIdMapper(override val idField: String = "_id") : IdMapper<ObjectNode> {
        override fun getId(obj: ObjectNode): String {
            return obj.get("_id").asText()
        }

        override fun setId(obj: ObjectNode, id: String): ObjectNode {
            obj.put("_id", id)
            return obj
        }

    }

}
