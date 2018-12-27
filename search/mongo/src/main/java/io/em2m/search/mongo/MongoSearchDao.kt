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

import com.mongodb.ReadPreference
import com.mongodb.ServerAddress
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.connection.ClusterSettings
import com.mongodb.rx.client.MongoClients
import com.mongodb.rx.client.MongoCollection
import io.em2m.search.core.daos.AbstractSearchDao
import io.em2m.search.core.model.*
import io.em2m.search.core.parser.SchemaMapper
import io.em2m.search.core.parser.SimpleSchemaMapper
import org.bson.Document
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import rx.Observable
import rx.Observable.just
import java.util.*

class MongoSearchDao<T>(idMapper: IdMapper<T>, val documentMapper: DocumentMapper<T>, val collection: MongoCollection<Document>, schemaMapper: SchemaMapper = SimpleSchemaMapper("")) :
        AbstractSearchDao<T>(idMapper) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val timeout = 60L

    val queryConverter = RequestConverter(schemaMapper)

    override fun create(entity: T): Observable<T> {
        val doc = encode(entity)
        doc.put("_id", generateId())
        return collection.insertOne(doc)
                .map { decodeItem(doc) }
    }

    override fun deleteById(id: String): Observable<Boolean> {
        return collection.deleteOne(Filters.eq("_id", id)).map { it.deletedCount > 0 }
    }

    fun deleteByQuery(query: Query): Observable<Long> {
        return collection.deleteMany(queryConverter.convertQuery(query)).map { it.deletedCount }
    }

    fun doSearch(request: SearchRequest, mongoQuery: Bson): Observable<List<Document>> {
        return if (request.limit > 0) {
            val fields = Document()
            request.fields.forEach { fields.put(it.name, "1") }
            collection.find(mongoQuery)
                    .projection(fields)
                    .sort(queryConverter.convertSorts(request.sorts))
                    .limit(request.limit.toInt()).skip(request.offset.toInt())
                    .toObservable().toList()
                    .doOnError {
                        log.error("Error in mongo!")
                    }
        } else Observable.just(emptyList())
    }

    fun doCount(request: SearchRequest, mongoQuery: Bson): Observable<Long> {
        return if (request.countTotal) {
            collection.count(mongoQuery)
        } else just(0L)
    }

    fun doAggs(request: SearchRequest, mongoQuery: Bson, mongoAggs: Bson): Observable<Document> {
        return if (request.aggs.isNotEmpty()) {
            collection
                    .withReadPreference(ReadPreference.secondary())
                    .aggregate(listOf(Aggregates.match(mongoQuery), mongoAggs)).toObservable()
        } else {
            just(null)
        }
    }

    fun handleResult(request: SearchRequest, docs: List<Document>, totalItems: Long, aggs: Document?): SearchResult<T> {
        val fields = request.fields
        val items = if (fields.isEmpty()) docs.map { decodeItem(it) } else null
        val rows = if (fields.isNotEmpty()) docs.map { decodeRow(request.fields, it) } else null
        return SearchResult(
                items = items, rows = rows,
                totalItems = if (totalItems > 0) totalItems else docs.size.toLong(),
                fields = fields,
                aggs = decodeAggs(aggs))
    }


    override fun search(request: SearchRequest): Observable<SearchResult<T>> {
        val mongoQuery = queryConverter.convertQuery(request.query ?: MatchAllQuery())
        val mongoAggs = queryConverter.convertAggs(request.aggs)
        try {
            return Observable.zip(
                    doSearch(request, mongoQuery), doCount(request, mongoQuery), doAggs(request, mongoQuery, mongoAggs)
            ) { docs, totalItems, aggs ->
                handleResult(request, docs, totalItems, aggs)
            }
                    .doOnError { err -> log.error("Error searching mongo", err) }
                    .doOnNext { results -> log.debug("results: " + results) }
        } catch (e: Exception) {
            return Observable.error(e)
        }
    }

    fun streamItems(request: SearchRequest): Observable<T> {
        val mongoQuery = queryConverter.convertQuery(request.query ?: MatchAllQuery())
        return if (request.limit > 0) {
            val fields = Document()
            request.fields.forEach { fields.put(it.name, "1") }
            collection.find(mongoQuery)
                    .projection(fields)
                    .sort(queryConverter.convertSorts(request.sorts))
                    .limit(request.limit.toInt()).skip(request.offset.toInt())
                    .toObservable()
                    .map { decodeItem(it) }
                    .doOnError {
                        log.error("Error in mongo!")
                    }
        } else Observable.empty()
    }

    fun streamRows(request: SearchRequest): Observable<List<Any?>> {
        val mongoQuery = queryConverter.convertQuery(request.query ?: MatchAllQuery())
        return if (request.limit > 0) {
            val fields = Document()
            request.fields.forEach { fields.put(it.name, "1") }
            collection.find(mongoQuery)
                    .projection(fields)
                    .sort(queryConverter.convertSorts(request.sorts))
                    .limit(request.limit.toInt()).skip(request.offset.toInt())
                    .toObservable()
                    .map { decodeRow(request.fields, it) }
                    .doOnError {
                        log.error("Error in mongo!")
                    }
        } else Observable.empty()
    }


    override fun save(id: String, entity: T): Observable<T> {
        return collection.replaceOne(Document("_id", id), encode(entity), UpdateOptions().upsert(true))
                /*.timeout(timeout, TimeUnit.SECONDS)*/
                .map { it -> entity }
    }

    fun bulkSave(entities: List<T>): Observable<BulkWriteResult> {
        val writes = entities.map { entity ->
            ReplaceOneModel(Document("_id", idMapper.getId(entity)), encode(entity), UpdateOptions().upsert(true))
        }

        return collection.bulkWrite(writes)
        /*.timeout(timeout, TimeUnit.SECONDS)*/
    }

    fun dropCollection(): Observable<Boolean> {
        return collection.drop().map({ true })
    }

    fun encode(value: T): Document {
        return documentMapper.toDocument(value)
    }

    fun decodeItem(doc: Document): T {
        return documentMapper.fromDocument(doc)
    }

    fun getFieldValue(field: String, doc: Document): Any? {
        return field.split(".").fold(doc as Any?, { value, property ->
            if (value is Map<*, *>) {
                value.get(property)
            } else value
        })
    }

    fun decodeRow(fields: List<Field>, doc: Document): List<Any?> {
        return fields.map { getFieldValue(requireNotNull(it.name), doc) }
    }

    fun decodeAggs(document: Document?): Map<String, AggResult> {
        if (document == null) return emptyMap()

        log.debug(document.toString())
        val result = HashMap<String, AggResult>()

        val keyIndex = document.keys.groupBy { key ->
            if (key.contains(":")) key.split(":")[0] else key
        }

        keyIndex.keys.forEach { key ->
            val buckets = ArrayList<Bucket>()
            var op: String? = null
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
            // TODO: Determine correct value for op
            result[key] = AggResult(key, buckets, op = op)
        }
        return result
    }

    companion object {
        fun collection(hostName: String, dbName: String, collectionName: String): MongoCollection<Document> {
            val settings = MongoClientSettings.builder()
                    .clusterSettings(ClusterSettings.builder().hosts(listOf(ServerAddress(hostName))).build())
                    .build()
            val client = MongoClients.create(settings)
            val database = client.getDatabase(dbName)
            return database.getCollection(collectionName)
        }
    }

}
