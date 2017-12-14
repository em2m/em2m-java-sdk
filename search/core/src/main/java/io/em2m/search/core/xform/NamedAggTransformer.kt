package io.em2m.search.core.xform

import io.em2m.search.core.model.Agg
import io.em2m.search.core.model.NamedAgg

class NamedAggTransformer(val namedAggs: Map<String, Agg>) : AggTransformer() {

    override fun transformNamedAgg(agg: NamedAgg): Agg {
        return namedAggs[agg.name]?.let { KeyAggTransformer(agg.key).transform(it) } ?: agg
    }
}