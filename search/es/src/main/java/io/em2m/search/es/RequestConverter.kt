package io.em2m.search.es

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.core.model.*

class RequestConverter(val objectMapper: ObjectMapper = jacksonObjectMapper()) {

    fun convert(request: SearchRequest): EsSearchRequest {

        val from = request.offset
        val size = request.limit
        val query = convertQuery(request.query)
        val fields = convertFields(request.fields)
        val aggs = convertAggs(request.aggs)
        val sort = convertSorts(request.sorts)

        return EsSearchRequest(from, size, query, fields, aggs, sort)
    }

    fun convertQuery(query: Query?): EsQuery = when (query) {
        is AndQuery -> {
            EsBoolQuery(must = query.of.map { convertQuery(it) })
        }
        is OrQuery -> {
            EsBoolQuery(should = query.of.map { convertQuery(it) })
        }
        is NotQuery -> {
            EsBoolQuery(mustNot = query.of.map { convertQuery(it) })
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
            EsRangeQuery(query.field, query.gte, query.gt, query.lte, query.lt, timeZone = query.timeZone)
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


    fun convertAggs(aggs: List<Agg>): EsAggs {
        val result = EsAggs()
        aggs.forEach {
            when (it) {
                is TermsAgg -> {
                    result.term(it.key, it.field, it.size, sortType(it.sort), sortDirection(it.sort), it.missing)
                }
                is MissingAgg -> {
                    result.missing(it.key, it.field)
                }
                is HistogramAgg -> {
                    result.histogram(it.key, it.field, it.interval, it.offset)
                }
                is StatsAgg -> {
                    result.stats(it.key, it.field)
                }
                is DateHistogramAgg -> {
                    result.dateHistogram(it.key, it.field, it.format, it.interval, it.offset, it.timeZone)
                }
                is RangeAgg -> {
                    val esRanges = result.agg(it.key, "range").put("field", it.field)
                            .withArray("ranges")
                    it.ranges.forEach {
                        esRanges.addPOJO(it)
                    }
                }
                is DateRangeAgg -> {
                    val esAgg = result.agg(it.key, "date_range").put("field", it.field)
                    if (it.format != null) esAgg.put("format", it.format)
                    if (it.timeZone != null) esAgg.put("timeZone", it.timeZone)
                    val esRanges = esAgg.withArray("ranges")
                    it.ranges.forEach {
                        esRanges.addPOJO(it)
                    }
                }
                is GeoHashAgg -> {
                    val esAgg = result.agg(it.key, "geohash_grid").put("field", it.field)
                    val precision = it.precision
                    val size = it.size
                    if (precision != null) esAgg.put("precision", precision)
                    if (size != null) esAgg.put("size", size)
                }
                is GeoCentroidAgg -> {
                    result.agg(it.key, "geo_centroid").put("field", it.field)
                }
                is GeoBoundsAgg -> {
                    result.agg(it.key, "geo_bounds").put("field", it.field)
                }
                is GeoDistanceAgg -> {
                    val esAgg = result.agg(it.key, "geo_distance").put("field", it.field)
                    esAgg.putPOJO("origin", it.origin)
                    if (it.unit != null) esAgg.put("unit", it.unit)
                    val esRanges = esAgg.withArray("ranges")
                    it.ranges.forEach {
                        esRanges.addPOJO(it)
                    }
                }
                is NativeAgg -> {
                    val value = it.value as? ObjectNode ?:
                            if (it.value is String) {
                                objectMapper.readTree(it.value as String) as ObjectNode
                            } else {
                                objectMapper.convertValue(it.value, ObjectNode::class.java)
                            }
                    result.agg(it.key, value)
                }
                is FiltersAgg -> {
                    val esAgg = result.agg(it.key, "filters")
                    val esFilters = esAgg.with("filters")
                    it.filters.forEach {
                        esFilters.putPOJO(it.key, convertQuery(it.value))
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

}