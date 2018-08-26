package io.em2m.search.core.xform

import io.em2m.search.core.model.AndQuery
import io.em2m.search.core.model.NotQuery
import io.em2m.search.core.model.OrQuery
import io.em2m.search.core.model.Query


class PushDownNotQueryTransformer : QueryTransformer() {

    override fun transformNotQuery(query: NotQuery): Query {
        return AndQuery(query.of.map { negate(it) })
    }

    fun negate(query: Query): Query {
        return when (query) {
            is OrQuery -> negateOr(query)
            is AndQuery -> negateAnd(query)
            else -> NotQuery(query)
        }
    }

    fun negateOr(query: OrQuery): Query {
        return AndQuery(query.of.map { negate(it) })
    }

    fun negateAnd(query: AndQuery): Query {
        return OrQuery(query.of.map { negate(it) })
    }

    fun negateNot(query: NotQuery): Query {
        return AndQuery(query.of)
    }

}