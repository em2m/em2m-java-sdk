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

import com.mongodb.MongoClient
import com.mongodb.ServerAddress
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.UpdateOptions
import io.em2m.search.core.daos.AbstractSyncDao
import io.em2m.search.core.model.*
import io.em2m.search.core.parser.SchemaMapper
import io.em2m.search.core.parser.SimpleSchemaMapper
import org.bson.Document
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import java.util.*

class MongoSyncDao<T>(idMapper: IdMapper<T>, val documentMapper: DocumentMapper<T>, val collection: MongoCollection<Document>, schemaMapper: SchemaMapper = SimpleSchemaMapper("")) :
        AbstractSyncDao<T>(idMapper) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val timeout = 10L

    val queryConverter = RequestConverter(schemaMapper)

    override fun create(entity: T): T? {
        val doc = encode(entity)
        doc.put("_id", generateId())
        collection.insertOne(doc)
        return decodeItem(doc)
    }

    override fun deleteById(id: String): Boolean {
        return collection.deleteOne(Filters.eq("_id", id)).deletedCount > 0
    }

    fun deleteByQuery(query: Query): Long {
        return collection.deleteMany(queryConverter.convertQuery(query)).deletedCount
    }

    fun doSearch(request: SearchRequest, mongoQuery: Bson): List<Document> {
        return if (request.limit > 0) {
            val fields = Document()
            request.fields.forEach { fields.put(it.name, "1") }
            collection.find(mongoQuery)
                    .projection(fields)
                    .sort(queryConverter.convertSorts(request.sorts))
                    .limit(request.limit.toInt()).skip(request.offset.toInt())
                    .toList()
        } else emptyList()
    }

    fun doCount(request: SearchRequest, mongoQuery: Bson): Long {
        return if (request.countTotal) {
            collection.count(mongoQuery)
        } else 0L
    }

    fun doAggs(request: SearchRequest, mongoQuery: Bson, mongoAggs: Bson): List<Document> {
        return if (request.aggs.isNotEmpty()) {
            collection.aggregate(listOf(Aggregates.match(mongoQuery), mongoAggs)).toList()
        } else {
            emptyList()
        }
    }

    fun handleResult(request: SearchRequest, docs: List<Document>, totalItems: Long, aggs: List<Document>): SearchResult<T> {
        val fields = request.fields
        val items = if (fields.isEmpty()) docs.map { decodeItem(it) } else null
        val rows = if (fields.isNotEmpty()) docs.map { decodeRow(request.fields, it) } else null
        return SearchResult(
                items = items, rows = rows,
                totalItems = if (totalItems > 0) totalItems else docs.size.toLong(),
                fields = fields,
                aggs = decodeAggs(aggs.firstOrNull()))
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        val mongoQuery = queryConverter.convertQuery(request.query ?: MatchAllQuery())
        val mongoAggs = queryConverter.convertAggs(request.aggs)
        val docs = doSearch(request, mongoQuery)
        val totalItems = doCount(request, mongoQuery)
        val aggs = doAggs(request, mongoQuery, mongoAggs)
        return handleResult(request, docs, totalItems, aggs)
    }

    fun streamItems(request: SearchRequest): List<T> {
        val mongoQuery = queryConverter.convertQuery(request.query ?: MatchAllQuery())
        return if (request.limit > 0) {
            val fields = Document()
            request.fields.forEach { fields[it.name] = "1" }
            collection.find(mongoQuery)
                    .projection(fields)
                    .sort(queryConverter.convertSorts(request.sorts))
                    .limit(request.limit.toInt()).skip(request.offset.toInt())
                    .toList()
                    .map { decodeItem(it) }
        } else emptyList()
    }

    fun streamRows(request: SearchRequest): List<Any?> {
        val mongoQuery = queryConverter.convertQuery(request.query ?: MatchAllQuery())
        return if (request.limit > 0) {
            val fields = Document()
            request.fields.forEach { fields[it.name] = "1" }
            collection.find(mongoQuery)
                    .projection(fields)
                    .sort(queryConverter.convertSorts(request.sorts))
                    .limit(request.limit.toInt()).skip(request.offset.toInt())
                    .toList()
                    .map { decodeRow(request.fields, it) }
        } else emptyList()
    }


    override fun save(id: String, entity: T): T? {
        collection.replaceOne(Document("_id", id), encode(entity), UpdateOptions().upsert(true))
        return entity
    }

    fun bulkSave(entities: List<T>): BulkWriteResult {
        val writes = entities.map { entity ->
            ReplaceOneModel(Document("_id", idMapper.getId(entity)), encode(entity), UpdateOptions().upsert(true))
        }
        return collection.bulkWrite(writes)
    }

    fun dropCollection(): Boolean {
        collection.drop()
        return true
    }

    fun encode(value: T): Document {
        return documentMapper.toDocument(value)
    }

    fun decodeItem(doc: Document): T {
        return documentMapper.fromDocument(doc)
    }

    fun getFieldValue(field: String, doc: Document): Any? {
        return field.split(".").fold(doc as Any?)
        { value, property ->
            if (value is Map<*, *>) {
                value.get(property)
            } else value
        }
    }

    fun decodeRow(fields: List<Field>, doc: Document): List<Any?> {
        return fields.map { getFieldValue(requireNotNull(it.name), doc) }
    }

    fun decodeAggs(document: Document?): Map<String, AggResult> {
        if (document == null) return emptyMap()

        log.debug(document.toString())
        val result = HashMap <String, AggResult>()

        val keyIndex = document.keys.groupBy { key ->
            if (key.contains(":")) key.split(":")[0] else key
        }

        keyIndex.keys.forEach { key ->
            val buckets = ArrayList<Bucket>()
            keyIndex[key]?.forEach { mongoKey ->
                val altKey = if (mongoKey.contains(":")) mongoKey.split(":")[1] else mongoKey
                val values = document[mongoKey] as List<*>
                values.forEach { value ->
                    if (value is Document) {
                        val id = value.getString("_id") ?: altKey
                        val count = value.getInteger("count")
                        buckets.add(Bucket(id, count.toLong()))
                    }
                }
            }
            result.put(key, AggResult(key, buckets))
        }
        return result
    }

    companion object {
        fun collection(hostName: String, dbName: String, collectionName: String): MongoCollection<Document> {

            /*val settings = MongoClientSettings.builder()
                    .clusterSettings(ClusterSettings.builder().hosts(listOf(ServerAddress(hostName))).build())
                    .build()*/

            val client = MongoClient(listOf(ServerAddress(hostName)))
            val database = client.getDatabase(dbName)
            return database.getCollection(collectionName)
        }
    }

}
