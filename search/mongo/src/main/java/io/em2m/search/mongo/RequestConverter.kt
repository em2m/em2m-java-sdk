package io.em2m.search.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Facet
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import io.em2m.search.core.model.*
import io.em2m.search.core.parser.LuceneExprParser
import io.em2m.search.core.parser.SchemaMapper
import io.em2m.simplex.parser.DateMathParser
import org.bson.Document
import org.bson.conversions.Bson
import org.joda.time.DateTimeZone
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class RequestConverter(private val schemaMapper: SchemaMapper, private val objectMapper: ObjectMapper = jacksonObjectMapper(), private val dateParser: DateMathParser = DateMathParser(DateTimeZone.UTC)) {

    private val notXform = PushDownNotQueryTransformer()

    private fun Query.fixNot() = notXform.transform(this)

    fun convertQuery(query: Query): Bson {
        return convertInternal(query.fixNot().simplify())
    }

    private fun convertInternal(query: Query?): Bson {

        return when (query) {
            is AndQuery -> {
                and(query.of.map { convertInternal(it) })
            }
            is OrQuery -> {
                or(query.of.map { convertInternal(it) })
            }
            is NotQuery -> {
                if (query.of.size == 1) {
                    Filters.not(convertInternal(query.of.first()))
                } else {
                    throw IllegalArgumentException("Not of multiple values is not supported")
                }
            }
            is MatchAllQuery -> {
                Document()
            }
            is TermQuery -> {
                val field = query.field
                val value = convertValue(field, query.value)
                Filters.eq<Any>(field, value)
            }
            is TermsQuery -> {
                val field = query.field
                val values = query.value.map { convertValue(field, it) }
                Filters.`in`<Any>(field, values)
            }
            is WildcardQuery -> {
                val field = schemaMapper.mapPath(query.field)
                when {
                    query.value == "*" -> Filters.ne(field, null)
                    field == "\$text" -> Filters.text(query.value)
                    else -> {
                        var value = query.value.replace("?", "_QUESTION_MARK_").replace("*", "_STAR_")
                        value = Matcher.quoteReplacement(value)
                        value = value.replace("_QUESTION_MARK_", ".?").replace("_STAR_", ".*")
                        val pattern = Pattern.compile(".*" + Matcher.quoteReplacement(value) + ".*", Pattern.CASE_INSENSITIVE)
                        Filters.regex(field, pattern)
                    }
                }
            }
            is MatchQuery -> {
                // TODO - This is not the correct implementation for Match Query - This is Wildcard Query
                val field = schemaMapper.mapPath(query.field)
                when {
                    query.value == "*" -> Filters.ne(field, null)
                    field == "\$text" -> Filters.text(query.value)
                    else -> {
                        var value = query.value.replace("?", "_QUESTION_MARK_").replace("*", "_STAR_")
                        value = Matcher.quoteReplacement(value)
                        value = value.replace("_QUESTION_MARK_", ".?").replace("_STAR_", ".*")
                        val pattern = Pattern.compile(".*" + Matcher.quoteReplacement(value) + ".*", Pattern.CASE_INSENSITIVE)
                        Filters.regex(field, pattern)
                    }
                }
            }
            is PrefixQuery -> {
                val pattern = Pattern.compile("^" + Matcher.quoteReplacement(query.value) + ".*", Pattern.CASE_INSENSITIVE)
                Filters.regex(query.field, pattern)
            }
            is RangeQuery -> {
                val field = query.field
                val fieldType = schemaMapper.typeOf(field) ?: String::class.java

                val expr = ArrayList<Bson>()
                if (Date::class.java.isAssignableFrom(fieldType)) {
                    val now = Date().time
                    query.lt?.let { expr.add(Filters.lt(field, parseDate(it.toString(), now, query.timeZone,false))) }
                    query.lte?.let { expr.add(Filters.lte(field, parseDate(it.toString(), now, query.timeZone, true))) }
                    query.gt?.let { expr.add(Filters.gt(field, parseDate(it.toString(), now, query.timeZone, true))) }
                    query.gte?.let { expr.add(Filters.gte(field, parseDate(it.toString(), now, query.timeZone, false))) }
                } else {
                    query.lt?.let { expr.add(Filters.lt(field, convertValue(field, it))) }
                    query.lte?.let { expr.add(Filters.lte(field, convertValue(field, it))) }
                    query.gt?.let { expr.add(Filters.gt(field, convertValue(field, it))) }
                    query.gte?.let { expr.add(Filters.gte(field, convertValue(field, it))) }
                }
                and(expr)
            }
            is DateRangeQuery -> {
                val field = query.field
                val expr = ArrayList<Bson>()
                val now = Date().time
                query.lt?.let { expr.add(Filters.lt(field, parseDate(it.toString(), now, query.timeZone,false))) }
                query.lte?.let { expr.add(Filters.lte(field, parseDate(it.toString(), now, query.timeZone, true))) }
                query.gt?.let { expr.add(Filters.gt(field, parseDate(it.toString(), now, query.timeZone, true))) }
                query.gte?.let { expr.add(Filters.gte(field, parseDate(it.toString(), now, query.timeZone, false))) }
                and(expr)
            }
            is PhraseQuery -> {
                val field = query.field
                val phrase = query.value.joinToString(" ")
                val pattern = Pattern.compile(Matcher.quoteReplacement(phrase), Pattern.CASE_INSENSITIVE)
                Filters.regex(field, pattern)
            }
            is RegexQuery -> {
                val pattern = Pattern.compile(query.value, Pattern.CASE_INSENSITIVE)
                Filters.regex(query.field, pattern)
            }
            is BboxQuery -> {
                val bbox = query.value
                Filters.geoWithinBox(query.field, bbox.minX, bbox.minY, bbox.maxX, bbox.maxY)
            }
            is LuceneQuery -> {
                convertInternal(LuceneExprParser("text").parse(query.query))
            }
            is NativeQuery -> {
                objectMapper.convertValue(query.value, Document::class.java)
            }
            is ExistsQuery -> {
                Filters.exists(query.field, query.value)
            }
            else -> {
                throw NotImplementedError("Query type (${query?.javaClass} Not supported")
            }
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

    private fun convertValue(path: String, value: Any?): Any? {
        var result: Any? = value
        if (value is String) {
            result = schemaMapper.valueOf(path, value)
        }
        return result
    }

    private fun and(filters: List<Bson>): Bson {
        return when {
            filters.isEmpty() -> Document()
            filters.size == 1 -> filters[0]
            else -> Filters.and(filters)
        }
    }

    private fun or(filters: List<Bson>): Bson {
        return when {
            filters.isEmpty() -> Document()
            filters.size == 1 -> filters[0]
            else -> Filters.or(filters)
        }
    }

    fun convertAggs(aggs: List<Agg>): Bson {
        val facets = ArrayList<Facet>()
        aggs.forEach { agg ->
            when (agg) {
                is TermsAgg -> {
                    facets.add(Facet(agg.key,
                            Document("\$unwind", "\$${agg.field}"),
                            Document("\$sortByCount", "\$${agg.field}"),
                            Document("\$limit", agg.size)))
                }
//                is HistogramAgg -> {
//                    Facet(it.key, Document(mapOf("\$bucketAuto" to mapOf("groupBy" to "_id", "buckets" to "4"))))
//                    //result.histogram(it.key, it.field, it.interval, it.offset)
//                }
//                is DateHistogramAgg -> {
//                    //result.dateHistogram(it.key, it.field, it.interval, it.offset, it.timeZone)
//                }
                is RangeAgg -> {
                    val key = agg.key
                    agg.ranges.forEach { range ->
                        val facetKey = "$key:${range.key}"
                        facets.add(Facet(facetKey,
                                Aggregates.match(convertInternal(RangeQuery(agg.field, gte = range.from, lt = range.to))),
                                Document(mapOf("\$group" to mapOf("_id" to null, "count" to mapOf("\$sum" to 1))))
                        ))
                    }
                }
                is DateRangeAgg -> {
                    val key = agg.key
                    agg.ranges.forEach { range ->
                        val facetKey = "$key:${range.key}"
                        facets.add(Facet(facetKey,
                                Aggregates.match(convertInternal(DateRangeQuery(agg.field, gte = range.from, lt = range.to))),
                                Document(mapOf("\$group" to mapOf("_id" to null, "count" to mapOf("\$sum" to 1))))
                        ))
                    }
                }
                is NativeAgg -> {
                    val value = when (agg.value) {
                        is Bson -> {
                            listOf(agg.value as Bson)
                        }
                        is String -> {
                            Document.parse(agg.value.toString()) as List<Bson>
                        }
                        is ArrayNode -> {
                            (agg.value as ArrayNode).map { objectMapper.convertValue(it, Document::class.java) }
                        }
                        else -> {
                            throw IllegalArgumentException("Unsupported native agg")
                        }
                    }
                    facets.add(Facet(agg.key, value))
                }
                is FiltersAgg -> {
                    val key = agg.key
                    agg.filters.forEach { filter ->
                        val facetKey = "$key:${filter.key}"
                        facets.add(Facet(facetKey,
                                Aggregates.match(convertInternal(filter.value)),
                                Document(mapOf("\$group" to mapOf("_id" to null, "count" to mapOf("\$sum" to 1))))
                        ))
                    }
                }
                else -> {
                    throw NotImplementedError(agg.javaClass.simpleName)
                }
            }
        }
        return Aggregates.facet(facets)
    }

    private fun parseDate(value: Any?, now: Long, timeZone: String?, roundUp: Boolean): Long? {
        return if (value is String) {
            try {
                Date(dateParser.parse(value, now, roundUp, DateTimeZone.forID(timeZone ?: "UTC"))).time
            } catch (ex: Exception) {
                objectMapper.convertValue<Date>(value).time
            }
        } else if (value != null) objectMapper.convertValue<Date>(value).time
        else null
    }

    class PushDownNotQueryTransformer : QueryTransformer() {

        override fun transformNotQuery(query: NotQuery): Query {
            return AndQuery(query.of.map { it.negate() })
        }

    }

}