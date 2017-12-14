package io.em2m.search.core.xform

import io.em2m.search.core.model.LuceneQuery
import io.em2m.search.core.model.Query
import io.em2m.search.core.parser.LuceneExprParser

class LuceneQueryTransformer() : QueryTransformer() {

    override fun transformLuceneQuery(query: LuceneQuery): Query {
        return LuceneExprParser(query.defaultField).parse(query.query)
    }

}