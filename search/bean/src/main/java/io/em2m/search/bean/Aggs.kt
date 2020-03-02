package io.em2m.search.bean

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.core.model.*
import io.em2m.simplex.parser.DateMathParser
import org.joda.time.DateTimeZone
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Aggs {

    companion object {

        val parsers = ConcurrentHashMap<String, DateMathParser>()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"))
        val objectMapper = jacksonObjectMapper()

        fun <T> processAggs(aggs: List<Agg>, matches: List<T>): Map<String, AggResult> {
            return aggs.map { agg ->
                when (agg) {
                    is TermsAgg -> {
                        processTermsAgg(agg, matches)
                    }
                    is RangeAgg -> {
                        processRangeAgg(agg, matches)
                    }
                    is DateHistogramAgg -> {
                        processDateHistogramAgg(agg, matches)
                    }
                    is HistogramAgg -> {
                        processHistogramAgg(agg, matches)
                    }
                    is DateRangeAgg -> {
                        processDateRangeAgg(agg, matches)
                    }
                    is FiltersAgg -> {
                        processFiltersAgg(agg, matches)
                    }
                    is StatsAgg -> {
                        processStatsAgg(agg, matches)
                    }
                    is MissingAgg -> {
                        processMissingAgg(agg, matches)
                    }
                    is GeoHashAgg -> {
                        processGeoHashAgg(agg, matches)
                    }
                    is GeoBoundsAgg -> {
                        processGeoBoundsAgg(agg, matches)
                    }
                    is GeoCentroidAgg -> {
                        processGeoCentroidAgg(agg, matches)
                    }
                    is GeoDistanceAgg -> {
                        processGeoDistanceAgg(agg, matches)
                    }
                    else -> {
                        throw UnsupportedOperationException("${agg.javaClass.simpleName} ")
                    }
                }
            }.associateBy { it.key }
        }

        private fun <T> processTermsAgg(agg: TermsAgg, matches: List<T>): AggResult {
            val value = null
            val countMap = HashMap<Any, Long>()
            val fieldGetter = Functions.field(agg.field)
            val missingKey = agg.missing
            var missingCount = 0L
            matches.forEach {
                val fieldValues = fieldGetter.invoke(it as Any)
                if (fieldValues.isEmpty()) {
                    ++missingCount
                }
                // only count each match once for each value
                fieldValues.distinct().forEach { value ->
                    // Add support for Missing
                    if (value != null) {
                        var count = countMap[value] ?: 0
                        count += 1
                        countMap[value] = count
                    }
                }
            }
            if (missingKey != null && missingCount > 0) {
                countMap[missingKey] = missingCount
            }
            val buckets = countMap.map {
                Bucket(it.key, it.value, it.key.toString())
            }
            // todo
            val sortedBuckets = sortAndFilter(agg, buckets).page(0, agg.size)
            return AggResult(agg.key, sortedBuckets, null, value, op = agg.op(), field = agg.field)
        }

        private fun <T> processDateRangeAgg(agg: DateRangeAgg, matches: List<T>): AggResult {
            val now = Date().time
            val buckets = agg.ranges.map { range ->
                val fromVal = range.from
                val toVal = range.to
                val from = parseDate(range.from, now, false, agg.timeZone)
                val to = parseDate(range.to, now, true, agg.timeZone)

                val predicate = Functions.toPredicate(RangeQuery(field = agg.field, gte = from, lt = to))
                val bucketMatches = matches.filter { predicate.invoke(it as Any) }
                val count = bucketMatches.size.toLong()
                val bucketAggs = processAggs(agg.aggs, bucketMatches)
                Bucket(range.key, count, from = fromVal, to = toVal, aggs = bucketAggs)
            }
            return AggResult(agg.key, buckets, op = agg.op(), field = agg.field)
        }

        private fun <T> processRangeAgg(agg: RangeAgg, matches: List<T>): AggResult {
            val buckets = agg.ranges.map { range ->
                val key = range.key
                val predicate = Functions.toPredicate(RangeQuery(field = agg.field, gte = range.from, lt = range.to))
                val count = matches.filter { predicate(it as Any) }.size.toLong()
                Bucket(key, count, from = range.from, to = range.to)
            }
            return AggResult(agg.key, buckets, op = agg.op(), field = agg.field)
        }

        private fun <T> processFiltersAgg(agg: FiltersAgg, matches: List<T>): AggResult {
            val buckets = agg.filters.map { (key, value) ->
                val predicate = Functions.toPredicate(value)
                val count = matches.filter { predicate.invoke(it as Any) }.size.toLong()
                Bucket(key, count, query = value)
            }
            return AggResult(agg.key, buckets, op = agg.op())
        }

        private fun <T> processDateHistogramAgg(agg: DateHistogramAgg, matches: List<T>): AggResult {
            val value = null
            val countMap = HashMap<Any, Long>()
            val bucketsMap = HashMap<String, MutableList<T>>()
            val fieldGetter = Functions.field(agg.field)
            val missingKey = agg.missing
            var missingCount = 0L
            val interval = agg.interval
            val now = Date().time

            val keyFormat = DateTimeFormatter.ofPattern(when (interval) {
                "year" -> "yyyy-01-01 00:00:00"
                "month" -> "yyyy-MM-01 00:00:00"
                "day" -> "yyyy-MM-dd 00:00:00"
                "hour" -> "yyyy-MM-dd HH:00:00"
                "minute" -> "yyyy-MM-dd HH:mm:00"
                "second" -> "yyyy-MM-dd HH:mm:ss"
                else -> throw IllegalArgumentException("Interval '$interval' not supported")
            }).withZone(ZoneId.of("UTC"))

            matches.forEach { match ->
                val fieldValues = fieldGetter.invoke(match as Any).mapNotNull { parseDate(it, now, false) }
                        .map { Date(it) }
                if (fieldValues.isEmpty()) {
                    ++missingCount
                }
                // only count each match once for each value
                fieldValues.distinct().forEach { value: Date ->

                    val key = keyFormat.format(value.toInstant())

                    var count = countMap[key] ?: 0
                    count += 1
                    bucketsMap.computeIfAbsent(key) { ArrayList() }.add(match)
                    countMap[key] = count
                }
            }
            if (missingKey != null && missingCount > 0) {
                countMap[missingKey] = missingCount
            }
            val buckets = countMap.map {
                val bucketAggs = processAggs(agg.aggs, bucketsMap[it.key] ?: emptyList())
                Bucket(it.key, it.value, it.key.toString(), aggs = bucketAggs)
            }

            // todo
            val sortedBuckets = sortAndFilter(agg, buckets, Agg.Sort(Agg.Sort.Type.Lexical, Direction.Ascending))

            sortedBuckets.map {
                val key = ZonedDateTime.parse(it.key as String, dateFormat).toInstant().toEpochMilli()
                Bucket(key = key, count = it.count, label = it.label,
                        stats = it.stats, from = it.from, to = it.to, query = it.query, aggs = it.aggs)
            }


            return AggResult(agg.key, sortedBuckets, null, value, op = agg.op(), field = agg.field)
        }

        private fun <T> processHistogramAgg(agg: HistogramAgg, matches: List<T>): AggResult {
            val value = null
            val countMap = HashMap<Any, Long>()
            val fieldGetter = Functions.field(agg.field)
            val missingKey = agg.missing
            var missingCount = 0L
            val offset = agg.offset ?: 0.0
            val interval = agg.interval
            matches.forEach { match ->
                val fieldValues = fieldGetter.invoke(match as Any).filter { it is Number }
                if (fieldValues.isEmpty()) {
                    ++missingCount
                }
                // only count each match once for each value
                fieldValues.distinct().mapNotNull { value -> (value as? Number)?.toDouble() }.forEach { value ->
                    val key = Math.floor((value - offset) / interval) * interval + offset
                    // Add support for Missing
                    var count = countMap[key] ?: 0
                    count += 1
                    countMap[key] = count
                }
            }
            if (missingKey != null && missingCount > 0) {
                countMap[missingKey] = missingCount
            }
            val buckets = countMap.map {
                Bucket(it.key, it.value, it.key.toString())
            }
            // todo
            val sortedBuckets = sortAndFilter(agg, buckets)
            return AggResult(agg.key, sortedBuckets, null, value, op = agg.op(), field = agg.field)
        }

        private fun <T> processStatsAgg(agg: StatsAgg, matches: List<T>): AggResult {

            var count: Long = 0
            var min: Double = Double.MAX_VALUE
            var max: Double = Double.MIN_VALUE
            var sum = 0.0

            val fieldGetter = Functions.field(agg.field)

            matches.forEach { match ->
                val fieldValues = fieldGetter.invoke(match as Any).filter { it is Number }
                fieldValues.forEach { value ->
                    try {
                        val d: Double = (value as Number).toDouble()
                        sum += d
                        count++
                        min = Math.min(min, d)
                        max = Math.max(max, d)
                    } catch (err: Throwable) {
                    }
                }
            }

            val stats = when (count) {
                0L -> Stats(count = count, sum = sum, min = null, max = null, avg = null)
                else -> Stats(count = count, sum = sum, min = min, max = max, avg = sum / count)
            }

            val bucket = Bucket(stats = stats, count = count)
            return AggResult(key = agg.key, buckets = listOf(bucket), op = agg.op(), field = agg.field)
        }

        private fun <T> processMissingAgg(agg: MissingAgg, matches: List<T>): AggResult {
            val fieldGetter = Functions.field(agg.field)
            var missingCount = 0L
            matches.forEach {
                val fieldValues = fieldGetter.invoke(it as Any)
                if (fieldValues.isEmpty()) {
                    ++missingCount
                }
            }
            val buckets = listOf(Bucket("missing", missingCount))
            return AggResult(agg.key, buckets, op = agg.op(), field = agg.field)
        }

        private fun <T> processGeoHashAgg(agg: GeoHashAgg, matches: List<T>): AggResult {
            throw NotImplementedError("Aggregation GeoHash not yet supported")
        }

        private fun <T> processGeoBoundsAgg(agg: GeoBoundsAgg, matches: List<T>): AggResult {
            throw NotImplementedError("Aggregation GeoBounds not yet supported")
        }

        private fun <T> processGeoCentroidAgg(agg: GeoCentroidAgg, matches: List<T>): AggResult {
            throw NotImplementedError("Aggregation GeoCentroid not yet supported")
        }

        private fun <T> processGeoDistanceAgg(agg: GeoDistanceAgg, matches: List<T>): AggResult {
            throw NotImplementedError("Aggregation GeoDistance not yet supported")
        }

        private fun sortAndFilter(agg: Agg, buckets: Collection<Bucket>, defaultSort: Agg.Sort = Agg.Sort(Agg.Sort.Type.Count, Direction.Descending)): List<Bucket> {
            val sort = agg.sort ?: defaultSort
            val minDocCount = agg.minDocCount ?: 1
            val filtered = buckets.filter { it.count >= minDocCount }
            return when (sort.type) {
                Agg.Sort.Type.Count -> {
                    if (sort.direction == Direction.Descending) {
                        filtered.sortedByDescending { it.count }
                    } else {
                        filtered.sortedBy { it.count }
                    }
                }
                Agg.Sort.Type.Lexical -> {
                    if (sort.direction == Direction.Descending) {
                        filtered.sortedByDescending { it.key.toString() }
                    } else {
                        filtered.sortedBy { it.key.toString() }
                    }
                }
                Agg.Sort.Type.None -> {
                    filtered.toList()
                }
            }
        }

        private fun parseDate(value: Any?, now: Long, roundUp: Boolean, timeZone: String? = null): Long? {
            return when {
                value is String -> try {
                    val parser = parsers.computeIfAbsent(timeZone ?: "UTC") {
                        val dtz = DateTimeZone.forID(timeZone) ?: DateTimeZone.UTC
                        DateMathParser(dtz)
                    }
                    parser.parse(value, now, roundUp = roundUp)
                } catch (ex: Exception) {
                    objectMapper.convertValue<Date>(value).time
                }
                value != null -> objectMapper.convertValue<Date>(value).time
                else -> null
            }
        }
    }
}