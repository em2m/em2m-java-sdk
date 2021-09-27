package io.em2m.search.core.xform

import io.em2m.search.core.model.LuceneQuery
import io.em2m.search.core.model.Query
import io.em2m.search.core.model.QueryTransformer
import io.em2m.search.core.parser.LuceneExprParser
import io.em2m.simplex.model.ExprContext

class LuceneQueryTransformer : QueryTransformer() {

    override fun transformLuceneQuery(query: LuceneQuery, context: ExprContext): Query {
        return LuceneExprParser(query.defaultField).parse(query.query)
    }

}
