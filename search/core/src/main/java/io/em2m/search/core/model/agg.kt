package io.em2m.search.core.model

import com.fasterxml.jackson.annotation.*
import com.vividsolutions.jts.geom.Coordinate

@JsonPropertyOrder("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "op")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonSubTypes(
        JsonSubTypes.Type(value = DateHistogramAgg::class, name = "date_histogram"),
        JsonSubTypes.Type(value = DateRangeAgg::class, name = "date_range"),
        JsonSubTypes.Type(value = FiltersAgg::class, name = "filters"),
        JsonSubTypes.Type(value = GeoBoundsAgg::class, name = "geo_bounds"),
        JsonSubTypes.Type(value = GeoCentroidAgg::class, name = "geo_centroid"),
        JsonSubTypes.Type(value = GeoDistanceAgg::class, name = "geo_distance"),
        JsonSubTypes.Type(value = GeoHashAgg::class, name = "geo_hash"),
        JsonSubTypes.Type(value = HistogramAgg::class, name = "histogram"),
        JsonSubTypes.Type(value = MissingAgg::class, name = "missing"),
        JsonSubTypes.Type(value = NamedAgg::class, name = "named"),
        JsonSubTypes.Type(value = NativeAgg::class, name = "native"),
        JsonSubTypes.Type(value = RangeAgg::class, name = "range"),
        JsonSubTypes.Type(value = StatsAgg::class, name = "stats"),
        JsonSubTypes.Type(value = TermsAgg::class, name = "terms")
)
abstract class Agg(val key: String, val sort: Sort? = null, val label: String? = null, val aggs: List<Agg> = emptyList(), ext: Map<String, Any?>?) {

    @JsonIgnore
    val extensions: MutableMap<String, Any?> = HashMap()

    @JsonAnyGetter
    fun anyGetter(): Map<String, Any?> {
        return extensions
    }

    @JsonAnySetter
    fun setAny(key: String, value: Any?): Agg {
        extensions[key] = value
        return this
    }

    init {
        if (ext != null) {
            extensions.putAll(ext)
        }
    }

    class Sort(val type: Type = Type.Count, val direction: Direction? = Direction.Descending) {

        companion object {

            fun lexicalDesc(): Sort {
                return Sort(Type.Lexical, Direction.Descending)
            }

            fun lexicalAsc(): Sort {
                return Sort(Type.Lexical, Direction.Ascending)
            }

            fun countDesc(): Sort {
                return Sort(Type.Count, Direction.Ascending)
            }

            fun countAsc(): Sort {
                return Sort(Type.Count, Direction.Ascending)
            }
        }

        enum class Type { Count, Lexical, None }
    }

}

data class Range(val to: Any? = null, val from: Any? = null, val key: String? = null)

class DateHistogramAgg(
        val field: String,
        val format: String? = null,
        val interval: String,
        val offset: String? = null,
        val timeZone: String? = null,
        key: String? = null,
        val missing: Any? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, ext = ext)

class DateRangeAgg(
        val field: String,
        val format: String? = null,
        val timeZone: String? = null,
        val ranges: List<Range>,
        key: String? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, ext = ext)

class FiltersAgg(
        val filters: Map<String, Query>,
        key: String,
        ext: Map<String, Any?>? = null) : Agg(key, ext = ext)

class GeoBoundsAgg(
        val field: String,
        key: String? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, ext = ext)

class GeoCentroidAgg(
        val field: String,
        key: String? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, ext = ext)

class GeoDistanceAgg(
        val field: String,
        val origin: Coordinate,
        val unit: String? = "mi",
        val ranges: List<Range>,
        key: String? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, ext = ext)

class GeoHashAgg(
        val field: String,
        val precision: Int? = null,
        val size: Int? = null,
        key: String? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, ext = ext)

class HistogramAgg(
        val field: String,
        val interval: Double,
        val offset: Double? = 0.0,
        key: String? = null,
        val missing: Any? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, ext = ext)

class MissingAgg(
        val field: String,
        key: String? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, ext = ext)

class NamedAgg(
        val name: String,
        key: String? = null,
        sort: Sort? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: name, sort, ext = ext)

class NativeAgg(
        val value: Any,
        key: String,
        ext: Map<String, Any?>? = null) : Agg(key, ext = ext)

class RangeAgg(
        val field: String,
        val ranges: List<Range>,
        key: String? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, ext = ext)

class StatsAgg(
        val field: String,
        key: String? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, ext = ext)

class TermsAgg(
        val field: String,
        val size: Int = 10,
        key: String? = null,
        sort: Sort? = null,
        val format: String? = null,
        val missing: Any? = null,
        ext: Map<String, Any?>? = null) : Agg(key ?: field, sort, ext = ext)

class Stats(val count: Long, val sum: Double, val min: Double, val max: Double, val avg: Double)

@JsonInclude(JsonInclude.Include.NON_NULL)
class Bucket(val key: Any? = null, val count: Long, val label: String? = null, val stats: Stats? = null, val from: Any? = null, val to: Any? = null, val aggs: Map<String, AggResult>? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
class AggResult(val key: String, val buckets: List<Bucket>? = null, val stats: Stats? = null, val value: Any? = null)
