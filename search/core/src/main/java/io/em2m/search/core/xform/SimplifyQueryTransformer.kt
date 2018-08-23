package io.em2m.search.core.xform

import io.em2m.search.core.model.AndQuery
import io.em2m.search.core.model.OrQuery
import io.em2m.search.core.model.Query


class SimplifyQueryTransformer : QueryTransformer() {

    override fun transformAndQuery(query: AndQuery): Query {
        return if (query.of.size == 1) {
            transform(query.of.first())
        } else {
            AndQuery(query.of.map(this::transform))
        }
    }

    override fun transformOrQuery(query: OrQuery): Query {
        return if (query.of.size == 1) {
            transform(query.of.first())
        } else {
            OrQuery(query.of.map(this::transform))
        }
    }
}