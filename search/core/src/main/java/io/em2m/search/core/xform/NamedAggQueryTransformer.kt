package io.em2m.search.core.xform

import io.em2m.search.core.model.Agg
import io.em2m.search.core.model.NamedQuery
import io.em2m.search.core.model.Query

class NamedQueryTransformer(val namedAggs: Map<String, Agg>) : QueryTransformer() {

    override fun transformNamedQuery(query: NamedQuery): Query {
        return super.transformNamedQuery(query)
    }

}