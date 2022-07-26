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
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Collation
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.UpdateOptions
import io.em2m.search.core.daos.AbstractSyncDao
import io.em2m.search.core.model.*
import io.em2m.search.core.parser.SchemaMapper
import io.em2m.search.core.parser.SimpleSchemaMapper
import org.bson.Document
import org.bson.conversions.Bson
import java.util.concurrent.ForkJoinPool
import java.util.stream.Collectors


class MongoSyncDao<T>(
    idMapper: IdMapper<T>,
    private val documentMapper: DocumentMapper<T>,
    val collection: MongoCollection<Document>,
    schemaMapper: SchemaMapper = SimpleSchemaMapper("")
) :
    AbstractSyncDao<T>(idMapper), StreamableDao<T> {

    private val queryConverter = RequestConverter(schemaMapper)

    override fun create(entity: T): T? {
        val doc = encode(entity)
        doc["_id"] = generateId()
        collection.insertOne(doc)
        return decodeItem(doc)
    }

    override fun deleteById(id: String): Boolean {
        return collection.deleteOne(Filters.eq("_id", id)).deletedCount > 0
    }

    fun deleteByQuery(query: Query): Long {
        return collection.deleteMany(queryConverter.convertQuery(query)).deletedCount
    }

    private fun doSearch(request: SearchRequest, mongoQuery: Bson): List<Document> {
        return if (request.limit > 0) {
            val fields = Document()
            request.fields.forEach { fields[it.name] = 1 }
            val locale: String? = request.params["locale"]?.toString()
            if (locale != null) {
                collection.find(mongoQuery)
                    .projection(fields)
                    .collation(Collation.builder().locale(locale).build())
                    .sort(queryConverter.convertSorts(request.sorts))
                    .limit(request.limit.toInt()).skip(request.offset.toInt())
                    .toList()
            } else {
                collection.find(mongoQuery)
                    .projection(fields)
                    .sort(queryConverter.convertSorts(request.sorts))
                    .limit(request.limit.toInt()).skip(request.offset.toInt())
                    .toList()
            }
        } else emptyList()
    }

    private fun doCount(request: SearchRequest, mongoQuery: Bson): Long {
        return if (request.countTotal) {
            collection.count(mongoQuery)
        } else 0L
    }

    private fun doAggs(request: SearchRequest, mongoQuery: Bson, mongoAggs: Bson): List<Document> {
        return if (request.aggs.isNotEmpty()) {
            collection
                .withReadPreference(ReadPreference.secondary())
                .aggregate(listOf(Aggregates.match(mongoQuery), mongoAggs)).toList()
        } else {
            emptyList()
        }
    }

    private fun handleResult(
        request: SearchRequest,
        docs: List<Document>,
        totalItems: Long,
        aggs: List<Document>
    ): SearchResult<T> {
        val fields = request.fields
        val items = if (fields.isEmpty()) docs.map { decodeItem(it) } else null
        val rows = if (fields.isNotEmpty()) docs.map { decodeRow(request.fields, it) } else null
        return SearchResult(
            items = items, rows = rows,
            totalItems = if (totalItems > 0) totalItems else docs.size.toLong(),
            fields = fields,
            aggs = decodeAggs(request, aggs.flatMap { doc -> doc.entries.map { (key, value) -> key to value } }.toMap())
        )
    }

    override fun search(request: SearchRequest): SearchResult<T> {
        val mongoQuery = queryConverter.convertQuery(request.query ?: MatchAllQuery())
        val docs = doSearch(request, mongoQuery)
        val totalItems: Long = doCount(request, mongoQuery)
        val aggs = if (request.aggs.size > 2 && false) {
            val customThreadPool = ForkJoinPool(request.aggs.size)
            val result = customThreadPool.submit<List<Document>> {
                request.aggs.parallelStream().map { agg ->
                    doAggs(request, mongoQuery, queryConverter.convertAggs(listOf(agg))).first()
                }.collect(Collectors.toList())
            }.get()
            customThreadPool.shutdown()
            result
        } else doAggs(request, mongoQuery, queryConverter.convertAggs(request.aggs))
        return handleResult(request, docs, totalItems, aggs)
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

    private fun encode(value: T): Document {
        return documentMapper.toDocument(value)
    }

    private fun decodeItem(doc: Document): T {
        return documentMapper.fromDocument(doc)
    }

    private fun getFieldValue(field: String, doc: Document): Any? {
        return field.split(".").fold(doc as Any?)
        { value, property ->
            if (value is Map<*, *>) {
                value[property]
            } else value
        }
    }

    private fun decodeRow(fields: List<Field>, doc: Document): List<Any?> {
        return fields.map { getFieldValue(requireNotNull(it.name), doc) }
    }

    private fun decodeAggs(request: SearchRequest, document: Map<String, Any>): Map<String, AggResult> {

        val result = HashMap<String, AggResult>()

        val aggIndex = request.aggs.associateBy { it.key }

        val keyIndex = document.keys.groupBy { key ->
            if (key.contains(":")) key.split(":")[0] else key
        }

        keyIndex.keys.forEach { key ->
            val buckets = ArrayList<Bucket>()
            val agg = aggIndex[key]

            keyIndex[key]?.forEachIndexed { index, mongoKey ->
                val altKey = if (mongoKey.contains(":")) mongoKey.split(":")[1] else mongoKey
                val values = document[mongoKey] as List<*>

                values.forEach { value ->
                    if (value is Document) {
                        val id = value.getString("_id") ?: altKey
                        val count = value.getInteger("count")

                        when (agg) {
                            is DateRangeAgg -> {
                                val range = agg.ranges[index]
                                buckets.add(Bucket(key = id, count = count.toLong(), from = range.from, to = range.to))
                            }
                            is RangeAgg -> {
                                val range = agg.ranges[index]
                                buckets.add(Bucket(key = id, count = count.toLong(), from = range.from, to = range.to))
                            }
                            else -> {
                                buckets.add(Bucket(id, count.toLong()))
                            }
                        }
                    }
                }
            }
            val op = aggIndex[key]?.op()
            result[key] = AggResult(key, buckets, op = op)
        }

        return result
    }

    override fun streamRows(
        fields: List<Field>,
        query: Query,
        sorts: List<DocSort>,
        params: Map<String, Any>
    ): Iterator<List<Any?>> {
        val mongoQuery = queryConverter.convertQuery(query)
        val mongoSorts = queryConverter.convertSorts(sorts)
        val projection = Document()
        fields.forEach { projection[it.name] = "1" }
        return collection.find(mongoQuery)
            .projection(projection)
            .sort(mongoSorts)
            .map { row ->
                decodeRow(fields, row)
            }.iterator()
    }

    override fun streamItems(query: Query, sorts: List<DocSort>, params: Map<String, Any>): Iterator<T> {
        val mongoQuery = queryConverter.convertQuery(query)
        val mongoSorts = queryConverter.convertSorts(sorts)
        return collection.find(mongoQuery)
            .sort(mongoSorts)
            .map { item ->
                decodeItem(item)
            }.iterator()
    }

}
