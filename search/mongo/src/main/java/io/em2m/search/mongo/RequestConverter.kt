package io.em2m.search.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Facet
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import io.em2m.search.core.model.*
import io.em2m.search.core.parser.LuceneExprParser
import io.em2m.search.core.parser.SchemaMapper
import org.bson.Document
import org.bson.conversions.Bson
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class RequestConverter(private val schemaMapper: SchemaMapper, val objectMapper: ObjectMapper = jacksonObjectMapper()) {

    fun convertQuery(query: Query?): Bson = when (query) {

        is AndQuery -> {
            and(query.of.map { convertQuery(it) })
        }
        is OrQuery -> {
            or(query.of.map { convertQuery(it) })
        }
        is NotQuery -> {
            Filters.not(and(query.of.map { convertQuery(it) }))
        }
        is MatchAllQuery -> {
            Document()
        }
        is TermQuery -> {
            val field = query.field
            val value = convertValue(field, query.value)
            Filters.eq<Any>(field, value)
        }
        is MatchQuery -> {
            val field = schemaMapper.mapPath(query.field)
            if (query.value == "*") {
                Filters.ne(field, null)
            } else if (field == "\$text") {
                Filters.text(query.value)
            } else {
                var value = query.value.replace("?", "_QUESTION_MARK_").replace("*", "_STAR_")
                value = Matcher.quoteReplacement(value)
                value = value.replace("_QUESTION_MARK_", ".?").replace("_STAR_", ".*")
                val pattern = Pattern.compile(".*" + Matcher.quoteReplacement(value) + ".*", Pattern.CASE_INSENSITIVE)
                Filters.regex(field, pattern)
            }
        }
        is PrefixQuery -> {
            val pattern = Pattern.compile("^" + Matcher.quoteReplacement(query.value) + ".*", Pattern.CASE_INSENSITIVE)
            Filters.regex(query.field, pattern)
        }
        is RangeQuery -> {
            val field = query.field
            val expr = ArrayList<Bson>()
            query.lt?.let { expr.add(Filters.lt(field, convertValue(field, it))) }
            query.lte?.let { expr.add(Filters.lte(field, convertValue(field, it))) }
            query.gt?.let { expr.add(Filters.gt(field, convertValue(field, it))) }
            query.gte?.let { expr.add(Filters.gte(field, convertValue(field, it))) }
            and(expr)
        }
        is PhraseQuery -> {
            val field = query.field
            val phrase = query.value.joinToString(" ")
            val pattern = Pattern.compile(Matcher.quoteReplacement(phrase), Pattern.CASE_INSENSITIVE)
            Filters.regex(field, pattern)
        }
        is BboxQuery -> {
            val bbox = query.value
            Filters.geoWithinBox(query.field, bbox.minX, bbox.minY, bbox.maxX, bbox.maxY)
        }
        is LuceneQuery -> {
            convertQuery(LuceneExprParser("text").parse(query.query))
        }
        is NativeQuery -> {
            objectMapper.convertValue(query.value, Document::class.java)
        }
        is ExistsQuery -> {
            Filters.exists(query.field, query.value ?: true)
        }
        else -> {
            throw NotImplementedError()
        }
    }

    fun convertSorts(sorts: List<DocSort>): Bson {
        return Sorts.orderBy(sorts.map {
            if (it.direction === Direction.Descending) {
                Sorts.descending(it.field)
            } else {
                Sorts.ascending(it.field)
            }
        })
    }

    fun convertValue(path: String, value: Any?): Any? {
        var result: Any? = value
        if (value is String) {
            result = schemaMapper.valueOf(path, value)
        }
        return result
    }

    fun and(filters: List<Bson>): Bson {
        return if (filters.isEmpty()) {
            Document()
        } else if (filters.size == 1) {
            filters[0]
        } else {
            Filters.and(filters)
        }
    }

    fun or(filters: List<Bson>): Bson {
        return if (filters.isEmpty()) {
            Document()
        } else if (filters.size == 1) {
            filters[0]
        } else {
            Filters.or(filters)
        }
    }

    fun convertAggs(aggs: List<Agg>): Bson {
        val facets = ArrayList<Facet>()
        aggs.forEach {
            when (it) {
                is TermsAgg -> {
                    facets.add(Facet(it.key,
                            Document("\$unwind", "\$${it.field}"),
                            Document("\$sortByCount", "\$${it.field}"),
                            Document("\$limit", it.size)))
                }
//                is HistogramAgg -> {
//                    Facet(it.key, Document(mapOf("\$bucketAuto" to mapOf("groupBy" to "_id", "buckets" to "4"))))
//                    //result.histogram(it.key, it.field, it.interval, it.offset)
//                }
//                is DateHistogramAgg -> {
//                    //result.dateHistogram(it.key, it.field, it.interval, it.offset, it.timeZone)
//                }
                is RangeAgg -> {
                    /*
                    val esRanges = result.agg(it.key, "range").put("field", it.field)
                            .withArray("ranges")
                    it.ranges.forEach {
                        esRanges.addPOJO(it)
                    }
                    */
                }
                is NativeAgg -> {
                    val value = when (it.value) {
                        is Bson -> {
                            listOf(it as Bson)
                        }
                        is String -> {
                            val parsed = Document.parse("{ \"pipe\": ${it.value}}")
                            parsed["pipe"] as List<Bson>
                        }
                        is ArrayNode -> {
                            (it.value as ArrayNode).map { objectMapper.convertValue(it, Document::class.java) }
                        }
                        else -> {
                            throw IllegalArgumentException("Unsupported native agg")
                        }
                    }
                    facets.add(Facet(it.key, value))
                }
                is FiltersAgg -> {
                    val key = it.key
                    it.filters.forEach {
                        val facetKey = "${key}:${it.key}"
                        facets.add(Facet(facetKey,
                                Aggregates.match(convertQuery(it.value)),
                                Document(mapOf("\$group" to mapOf("_id" to null, "count" to mapOf("\$sum" to 1))))
                        ))
                    }
                }
                else -> {
                    throw NotImplementedError()
                }
            }
        }
        return Aggregates.facet(facets)
    }

}