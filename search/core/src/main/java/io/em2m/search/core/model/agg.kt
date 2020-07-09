package io.em2m.search.core.model

import com.fasterxml.jackson.annotation.*
import org.locationtech.jts.geom.Coordinate

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
        JsonSubTypes.Type(value = TermsAgg::class, name = "terms"),
        JsonSubTypes.Type(value = XformAgg::class, name = "xform")
)
abstract class Agg(
        val key: String,
        val sort: Sort? = null,
        val label: String? = null,
        val aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>?,
        val minDocCount: Int?) {

    @JsonIgnore
    val extensions: MutableMap<String, Any?> = ext?.toMutableMap() ?: HashMap()

    @JsonAnyGetter
    fun anyGetter(): Map<String, Any?> {
        return extensions
    }

    @JsonAnySetter
    fun setAny(key: String, value: Any?): Agg {
        extensions[key] = value
        return this
    }

    abstract fun op(): String

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

interface Fielded {
    val field: String?
}

data class Range(val to: Any? = null, val from: Any? = null, val key: String? = null)

class DateHistogramAgg(
        override val field: String,
        val format: String? = null,
        val interval: String,
        val offset: String? = null,
        val timeZone: String? = null,
        val missing: Any? = null,
        key: String? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: field, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "date_histogram"
}

class DateRangeAgg(
        override val field: String,
        val format: String? = null,
        val timeZone: String? = null,
        val ranges: List<Range>,
        key: String? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: field, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "date_range"
}

class FiltersAgg(
        val filters: Map<String, Query>,
        key: String,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key, aggs = aggs, ext = ext, minDocCount = minDocCount) {
    override fun op() = "filters"
}

class GeoBoundsAgg(
        override val field: String,
        key: String? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: field, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "geo_bounds"
}

class GeoCentroidAgg(
        override val field: String,
        key: String? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: field, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "geo_centroid"
}

class GeoDistanceAgg(
        override val field: String,
        val origin: Coordinate,
        val unit: String? = "mi",
        val ranges: List<Range>,
        key: String? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: field, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "geo_distance"
}

class GeoHashAgg(
        override val field: String,
        val precision: Int? = null,
        val size: Int? = null,
        key: String? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: field, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "geohash"
}

class HistogramAgg(
        override val field: String,
        val format: String? = null,
        val interval: Double,
        val offset: Double? = 0.0,
        key: String? = null,
        val missing: Any? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: field, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "histogram"
}

class MissingAgg(
        override val field: String,
        key: String? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: field, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "missing"
}

class NamedAgg(
        val name: String,
        key: String? = null,
        sort: Sort? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: name, sort, aggs = aggs, ext = ext, minDocCount = minDocCount) {
    override fun op() = "named"
}

class NativeAgg(
        val value: Any,
        key: String,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key, aggs = aggs, ext = ext, minDocCount = minDocCount) {
    override fun op() = "native"
}

class RangeAgg(
        override val field: String,
        val ranges: List<Range>,
        key: String? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: field, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "range"
}

class StatsAgg(
        override val field: String,
        key: String? = null,
        val format: String? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key ?: field, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "stats"
}

class TermsAgg(
        override val field: String,
        val size: Int = 10,
        key: String? = null,
        sort: Sort? = null,
        val format: String? = null,
        val missing: Any? = null,
        aggs: List<Agg> = emptyList(),
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null) : Agg(key
        ?: field, sort, aggs = aggs, ext = ext, minDocCount = minDocCount), Fielded {
    override fun op() = "terms"
}

class XformAgg(
        key: String? = null,
        sort: Sort? = null,
        val agg: Agg,
        ext: Map<String, Any?>? = null,
        minDocCount: Int? = null,
        val bucket: Any? = null
) : Agg(key = key ?: agg.key, ext = ext, minDocCount = minDocCount, sort = sort) {
    override fun op() = "xform"
}

data class Stats(val count: Long, val sum: Double, val min: Double?, val max: Double?, val avg: Double?)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Bucket(val key: Any? = null, val count: Long, val label: String? = null, val stats: Stats? = null, val from: Any? = null, val to: Any? = null, val query: Query? = null, val aggs: Map<String, AggResult>? = null, val value: Any? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AggResult(val key: String, val buckets: List<Bucket>? = null, val stats: Stats? = null, val value: Any? = null, val op: String? = null, val field: String? = null)
