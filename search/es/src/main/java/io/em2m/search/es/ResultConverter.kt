package io.em2m.search.es

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.scaleset.geo.geojson.GeoJsonModule
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Envelope
import io.em2m.search.core.model.*
import java.util.*
import kotlin.collections.HashMap

class ResultConverter<T>(private val mapper: DocMapper<T>) {

    fun convert(request: SearchRequest, result: EsSearchResult): SearchResult<T> {
        val hits = result.hits
        val rows = if (request.fields.isNotEmpty()) convertRows(result.hits, request) else null
        val items = if (request.fields.isEmpty()) convertItems(result.hits) else null
        val totalItems = hits.total
        val aggs = convertAggs(request.aggs, result.aggregations)

        val headers = mapOf("took" to result.took, "scrollId" to result.scrollId)

        return SearchResult(aggs, items, rows, totalItems = totalItems, fields = request.fields, headers = headers)

//        throw NotImplementedError()
//
//        @Throws(Exception::class)
//        fun convert(): Results<T> {
//            initialize()
//            addItems()
//            addAggregations()
//            addHeaders()
//            val fields: List<String>? = null
//            val results = Results<T>(query, aggs, items, totalItems, null, headers, fields)
//            return results
//        }

    }

    private fun convertItems(hits: EsHits): List<T> {
        val results = ArrayList<T>()
        for (hit in hits.hits) {
            val item = convertItem(hit)
            if (item != null) {
                results.add(item)
            }
        }
        return results
    }

    private fun convertItem(hit: EsHit): T? {
        return try {
            hit.source?.let { mapper.fromDoc(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun convertRows(hits: EsHits, request: SearchRequest): List<List<Any?>> {
        val results = ArrayList<List<Any?>>()
        for (hit in hits.hits) {
            if (hit.fields != null) {
                val row = request.fields.map { field ->
                    val values = hit.fields[field.name] ?: emptyList()
                    // TODO: Deal with field aliases
                    val value = when {
                        values.isEmpty() -> null
                        values.size == 1 -> mapper.toObject(values[0])
                        else -> // definitely a hack!
                            mapper.toObject(JsonNodeFactory.instance.arrayNode().addAll(values))
                    }
                    value
                }
                results.add(row)
            }
        }
        return results
    }

    private fun convertAggs(aggs: List<Agg>, esAggResults: Map<String, EsAggResult>): Map<String, AggResult> {
        return aggs.mapNotNull { agg ->
            val key = agg.key
            val esValue = esAggResults[agg.key]
            if (esValue != null) {
                val op: String? = agg.op()
                val field: String? = (agg as? Fielded)?.field
                var buckets = esValue.buckets?.map {
                    val subAggs = convertSubAggs(agg.aggs, it.other)
                    val buckeyKey = it.keyAsString ?: it.key
                    Bucket(buckeyKey, it.docCount?.toLong() ?: 0, from = it.from, to = it.to, aggs = subAggs)
                }
                if (buckets != null && agg is DateRangeAgg) {
                    buckets = sortRangeBuckets(agg.ranges, buckets)
                }
                if (buckets != null && agg is RangeAgg) {
                    buckets = sortRangeBuckets(agg.ranges, buckets)
                }
                var value: Any? = null
                val docCount = esValue.docCount
                if (buckets == null && docCount != null) {
                    buckets = listOf(Bucket(key = key, count = docCount.toLong()))
                }
                // stats
                if (buckets == null && esValue.count != null) {
                    val count = esValue.count.toLong()
                    val sum = esValue.sum ?: 0.0
                    val min = esValue.min ?: 0.0
                    val max = esValue.max ?: 0.0
                    val avg = esValue.avg ?: 0.0
                    val stats = Stats(count, sum, min, max, avg)
                    buckets = listOf(Bucket(key = key, count = count, stats = stats))
                }
                if (esValue.other.containsKey("location")) {
                    val location = esValue.other["location"] as Map<*, *>
                    val lat = location["lat"] as Double
                    val lon = location["lon"] as Double
                    value = Coordinate(lon, lat)
                }
                if (buckets == null && esValue.other.containsKey("bounds")) {
                    val bounds = esValue.other["bounds"] as Map<*, *>
                    val topLeft = bounds["top_left"] as Map<*, *>
                    val bottomRight = bounds["bottom_right"] as Map<*, *>
                    val x1 = topLeft["lon"] as Double
                    val x2 = bottomRight["lon"] as Double
                    val y1 = topLeft["lat"] as Double
                    val y2 = bottomRight["lat"] as Double
                    value = Envelope(x1, x2, y1, y2)
                }
                if (buckets == null && value == null) {
                    value = esValue.other
                }
                AggResult(key, buckets, value = value, op = op, field = field)
            } else null
        }.associateBy { it.key }
    }

    private fun sortRangeBuckets(ranges: List<Range>, buckets: List<Bucket>): List<Bucket> {
        // sort keyed buckets first in their original, then preserve resulting order for non-keyed buckets
        val order = ranges.mapIndexed { index, range -> range.key to index }.toMap()
        return buckets.mapIndexed { index, bucket -> index to bucket }.sortedBy {
            order[it.second.key] ?: buckets.size+it.first
        }.map { it.second }
    }

    private fun convertSubAggs(aggs: List<Agg>, other: Map<String, Any?>): Map<String, AggResult> {
        return try {
            val esAggs: MutableMap<String, EsAggResult> = HashMap()
            other.forEach { key, value ->
                val aggResult = objectMapper.convertValue(value, EsAggResult::class.java)
                if (aggResult != null) {
                    esAggs[key] = aggResult
                }
            }
            convertAggs(aggs, esAggs)
        } catch (ex: Throwable) {
            emptyMap()
        }
    }

    companion object {
        // TODO: Make this configurable
        val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
    }
}