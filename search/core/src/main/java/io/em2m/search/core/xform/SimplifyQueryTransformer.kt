package io.em2m.search.core.xform

import io.em2m.search.core.model.AndQuery
import io.em2m.search.core.model.MatchAllQuery
import io.em2m.search.core.model.OrQuery
import io.em2m.search.core.model.Query


class SimplifyQueryTransformer : QueryTransformer() {

    override fun transformAndQuery(query: AndQuery): Query {
        val of = query.of.filter { it !is MatchAllQuery }

        return when {
            of.isEmpty() -> MatchAllQuery()
            of.size == 1 -> transform(of.first())
            else -> {
                AndQuery(of.map(this::transform))
            }
        }
    }

    override fun transformOrQuery(query: OrQuery): Query {
        val of = query.of
        val matchAllCount = of.count { it is MatchAllQuery }

        return when {
            matchAllCount > 0 -> MatchAllQuery()
            of.isEmpty() -> MatchAllQuery()
            of.size == 1 -> transform(of.first())
            else -> OrQuery(of.map(this::transform))
        }
    }
}