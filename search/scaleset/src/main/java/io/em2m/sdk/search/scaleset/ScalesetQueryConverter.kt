package io.em2m.sdk.search.scaleset

import com.scaleset.search.Filter
import com.scaleset.search.Sort
import com.vividsolutions.jts.geom.Envelope
import io.em2m.search.core.model.*
import com.scaleset.search.Aggregation as ScalesetAggregation
import com.scaleset.search.Query as ScalesetQuery


class ScalesetQueryConverter {


    fun convertQuery(scalesetQuery: ScalesetQuery): SearchRequest {
        val offset = scalesetQuery.offset.toLong()
        val limit = scalesetQuery.offset.toLong()
        val sorts = convertSorts(scalesetQuery.sorts)
        val query = convertFilters(scalesetQuery.q, scalesetQuery.bbox, scalesetQuery.geoField, scalesetQuery.filters)
        val headers = convertHeaders(scalesetQuery.headers)
        val aggs = convertAggregations(scalesetQuery.aggs)
        val fieldSet = scalesetQuery.fieldSet
        val fields = convertFields(scalesetQuery.fields)

        return SearchRequest(offset, limit, query, headers, fieldSet, fields, sorts, aggs)
    }

    fun convertSorts(sorts: Array<Sort>): List<DocSort> {
        return sorts.map {
            val direction = if (it.direction == Sort.Direction.Ascending) Direction.Ascending else Direction.Descending
            DocSort(it.field, direction)
        }

    }

    fun convertFields(fields: Array<String>): List<Field> {
        return fields.map { Field(it) }
    }

    fun convertHeaders(headers: Map<String, Any?> = emptyMap()): Map<String, Any> {
        return headers.mapNotNull({ it.key to (it.value as Any) }).associate { it }
    }

    fun convertFilters(q: String?, bbox: Envelope?, geoField: String?, filters: Map<String, Filter> = emptyMap()): Query {
        val queries = ArrayList<Query>()
        if (q != null) {
            queries.add(LuceneQuery(q))
        }
        if (bbox != null && geoField != null) {
            queries.add(BboxQuery(geoField, bbox))
        }
        queries.addAll(filters.values.map { convertFilter(it) })
        return AndQuery(queries)
    }

    fun convertFilter(filter: Filter): Query {
        return when (filter.type) {
            "term" -> convertTermFilter(filter)
            "range" -> convertRangeFilter(filter)
            "query" -> convertQueryFilter(filter)
            "prefix" -> convertPrefixyFilter(filter)
            "geo_bounding_box" -> convertGeoBoundingBoxFilter(filter)
            else -> throw IllegalArgumentException("Unsupported filter type")
        }
    }

    fun convertAggregations(aggs: Map<String, ScalesetAggregation>): List<Agg> {
        return aggs.map { (key, agg) ->
            val type = agg.type
            when (type) {
                "term" -> {
                    TermsAgg(key, sort = null)
                }
                else -> throw IllegalArgumentException("Unsupported aggregation type")
            }
        }
    }

    private fun convertQueryFilter(filter: Filter): LuceneQuery {
        val value = filter.getString("query")
        return LuceneQuery(value)
    }

    private fun convertPrefixyFilter(filter: Filter): PrefixQuery {
        val field = filter.getString("field")
        val value = filter.getString("query")
        return PrefixQuery(field, value)
    }

    private fun convertGeoBoundingBoxFilter(filter: Filter): BboxQuery {
        val name = filter.name
        val field = filter.getString("field")
        val bbox = filter.get(Envelope::class.java, "bbox")
        return BboxQuery(name ?: field, bbox)
    }

    private fun convertTermFilter(filter: Filter): TermQuery {
        val field = filter.getString("field")
        val value = filter.getString("query")
        return TermQuery(field, value)
    }

    private fun convertRangeFilter(filter: Filter): RangeQuery {

        var field = filter.getString("field")
        if (field == null) {
            field = filter.name
        }
        val gte = filter.getString("gte")
        val gt = filter.getString("gt")
        val lt = filter.getString("lt")
        val lte = filter.getString("lte")
        val timeZone = filter.getString("time_zone")

        return RangeQuery(field, lt = lt, gt = gt, gte = gte, lte = lte)
    }

}
