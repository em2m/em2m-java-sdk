package io.em2m.search.es

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.core.model.*
import io.em2m.simplex.parser.DateMathParser
import org.joda.time.DateTimeZone
import java.util.*

class RequestConverter(val objectMapper: ObjectMapper = jacksonObjectMapper(), val es6: Boolean = false) {

    fun convert(request: SearchRequest): EsSearchRequest {

        val from = request.offset
        val size = request.limit
        val query = convertQuery(request.query, request.params)
        val fields = convertFields(request.fields)
        val aggs = convertAggs(request.aggs, request.params)
        val sort = convertSorts(request.sorts)

        return when {
            es6 -> EsSearchRequest(from, size, query, aggs = aggs, sort = sort, storedFields = fields)
            else -> EsSearchRequest(from, size, query, fields, aggs, sort)
        }

    }

    fun convertQuery(query: Query?, params: Map<String, Any>?): EsQuery = when (query) {
        is AndQuery -> {
            EsBoolQuery(must = query.of.map { convertQuery(it, params) })
        }
        is OrQuery -> {
            EsBoolQuery(should = query.of.map { convertQuery(it, params) })
        }
        is NotQuery -> {
            EsBoolQuery(mustNot = query.of.map { convertQuery(it, params) })
        }
        is MatchAllQuery -> {
            EsMatchAllQuery()
        }
        is TermQuery -> {
            EsTermQuery(query.field, query.value.toString())
        }
        is MatchQuery -> {
            EsMatchQuery(query.field, query.value, query.operator)
        }
        is RegexQuery -> {
            EsRegexpQuery(query.field, query.value)
        }
        is PrefixQuery -> {
            EsPrefixQuery(query.field, query.value)
        }
        is RangeQuery -> {
            // format support?
            // boost?
            val timeZone = query.timeZone ?: params?.get("timeZone")?.toString()

            if (query.gt is String || query.gte is String || query.lt is String || query.lte is String) {
                EsRangeQuery(query.field, query.gte, query.gt, query.lte, query.lt, timeZone = timeZone)
            } else {
                EsRangeQuery(query.field, query.gte, query.gt, query.lte, query.lt)
            }
        }
        is BboxQuery -> {
            EsGeoBoundingBoxQuery(query.field, query.value)
        }
        is ExistsQuery -> {
            if (query.value) {
                EsExistsQuery(query.field)
            } else {
                EsBoolQuery(mustNot = listOf(EsExistsQuery(query.field)))
            }
        }
        is LuceneQuery -> {
            EsQueryStringQuery(query.query, query.defaultField, "and")
        }
        else -> {
            throw NotImplementedError("Unsupported query type: ${query?.javaClass?.name.toString()}")
        }
    }

    fun sortType(sort: Agg.Sort?): EsSortType {
        return if (sort?.type == Agg.Sort.Type.Lexical) {
            EsSortType.TERM
        } else {
            EsSortType.COUNT
        }
    }

    fun sortDirection(sort: Agg.Sort?): EsSortDirection {
        return if (sort?.direction == Direction.Ascending) {
            EsSortDirection.ASC
        } else {
            EsSortDirection.DESC
        }
    }


    fun convertAggs(aggs: List<Agg>, params: Map<String, Any>): EsAggs {
        val result = EsAggs()
        val timeZone = DateTimeZone.forID(params["timeZone"] as? String)
        aggs.forEach {
            val subAggs = if (it.aggs?.isNotEmpty()) {
                convertAggs(it.aggs, params)
            } else {
                null
            }
            when (it) {
                is TermsAgg -> {
                    result.term(it.key, it.field, it.size, sortType(it.sort), sortDirection(it.sort), it.missing, subAggs).minDocCount(it.minDocCount)
                }
                is MissingAgg -> {
                    result.missing(it.key, it.field, subAggs).minDocCount(it.minDocCount)
                }
                is HistogramAgg -> {
                    result.histogram(it.key, it.field, it.interval, it.offset, subAggs).minDocCount(it.minDocCount)
                }
                is StatsAgg -> {
                    result.stats(it.key, it.field).minDocCount(it.minDocCount)
                }
                is DateHistogramAgg -> {
                    result.dateHistogram(it.key, it.field, it.format, it.interval, it.offset, timeZone.id, subAggs).minDocCount(it.minDocCount)
                }
                is RangeAgg -> {
                    val esRanges = result.agg(it.key, "range", subAggs).put("field", it.field).minDocCount(it.minDocCount)
                            .withArray("ranges")
                    it.ranges.forEach {
                        esRanges.addPOJO(it)
                    }
                }
                is DateRangeAgg -> {
                    val esAgg = result.agg(it.key, "date_range", subAggs).put("field", it.field).minDocCount(it.minDocCount)
                    if (it.format != null) esAgg.put("format", it.format)
                    //if (it.timeZone != null) esAgg.put("time_zone", timeZone.id)
                    val esRanges = esAgg.withArray("ranges")

                    val dateMathParser = DateMathParser(timeZone)
                    it.ranges.forEach { range ->
                        if (timeZone != null) {
                            val now = Date()
                            val from = if (range.from is String)
                                dateMathParser.parse(range.from as String, now.time, false, timeZone)
                            else
                                range.from

                            val to = if (range.to is String)
                                dateMathParser.parse(range.to as String, now.time, true, timeZone)
                            else
                                range.to

                            esRanges.addPOJO(mapOf("key" to range.key, "from" to from, "to" to to))
                        } else {
                            esRanges.addPOJO(range)
                        }
                    }
                }
                is GeoHashAgg -> {
                    val esAgg = result.agg(it.key, "geohash_grid", subAggs).put("field", it.field).minDocCount(it.minDocCount)
                    val precision = it.precision
                    val size = it.size
                    if (precision != null) esAgg.put("precision", precision)
                    if (size != null) esAgg.put("size", size)
                }
                is GeoCentroidAgg -> {
                    result.agg(it.key, "geo_centroid", subAggs).put("field", it.field).minDocCount(it.minDocCount)
                }
                is GeoBoundsAgg -> {
                    result.agg(it.key, "geo_bounds", subAggs).put("field", it.field).minDocCount(it.minDocCount)
                }
                is GeoDistanceAgg -> {
                    val esAgg = result.agg(it.key, "geo_distance", subAggs).put("field", it.field).minDocCount(it.minDocCount)
                    esAgg.putPOJO("origin", it.origin)
                    if (it.unit != null) esAgg.put("unit", it.unit)
                    val esRanges = esAgg.withArray("ranges")
                    it.ranges.forEach {
                        esRanges.addPOJO(it)
                    }
                }
                is NativeAgg -> {
                    val value = it.value as? ObjectNode ?: if (it.value is String) {
                        objectMapper.readTree(it.value as String) as ObjectNode
                    } else {
                        objectMapper.convertValue(it.value, ObjectNode::class.java)
                    }
                    result.agg(it.key, value).minDocCount(it.minDocCount)
                }
                is FiltersAgg -> {
                    val esAgg = result.agg(it.key, "filters", subAggs)
                    val esFilters = esAgg.with("filters").minDocCount(it.minDocCount)
                    it.filters.forEach {
                        esFilters.putPOJO(it.key, convertQuery(it.value, params))
                    }
                }
                else -> {
                    throw NotImplementedError()
                }
            }
        }
        return result
    }

    fun convertFields(fields: List<Field>): List<String> {
        return fields.map { requireNotNull(it.name, { "Field name cannot be null" }) }.sorted().distinct()
    }

    fun convertSorts(sorts: List<DocSort>): List<Map<String, String>> {
        return sorts.map { sort -> mapOf(sort.field to if (sort.direction == Direction.Ascending) "asc" else "desc") }
    }

    private fun ObjectNode.minDocCount(minDocCount: Int?): ObjectNode {
        if (minDocCount != null) {
            this.put("min_doc_count", minDocCount)
        }
        return this
    }

}
