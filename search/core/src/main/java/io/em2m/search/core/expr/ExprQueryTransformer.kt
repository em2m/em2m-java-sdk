package io.em2m.search.core.expr

import io.em2m.search.core.model.*
import io.em2m.simplex.parser.ExprParser

@Deprecated("Use FieldTransformerDao instead")
class ExprQueryTransformer(private val parser: ExprParser) : QueryTransformer() {

    private fun <T : Query> mapExpr(field: String, query: T, fn: (String, T) -> Query): Query {
        return if (field.contains("\${")) {
            val expr = parser.parse(field)
            val fields = FieldKeyHandler.fields(expr)
            val queries = fields.map { fn(it, query) }
            return if (queries.size == 1) {
                queries.first()
            } else OrQuery(queries)
        } else query
    }

    override fun transformTermQuery(query: TermQuery) = mapExpr(query.field, query) { f, q -> TermQuery(f, q.value) }
    override fun transformTermsQuery(query: TermsQuery) = mapExpr(query.field, query) { f, q -> TermsQuery(f, q.value) }
    override fun transformMatchQuery(query: MatchQuery) = mapExpr(query.field, query) { f, q -> MatchQuery(f, q.value, q.operator) }
    override fun transformPhraseQuery(query: PhraseQuery) = mapExpr(query.field, query) { f, q -> PhraseQuery(f, q.value) }
    override fun transformPrefixQuery(query: PrefixQuery) = mapExpr(query.field, query) { f, q -> PrefixQuery(f, q.value) }
    override fun transformWildcardQuery(query: WildcardQuery) = mapExpr(query.field, query) { f, q -> WildcardQuery(f, q.value) }
    override fun transformRegexQuery(query: RegexQuery) = mapExpr(query.field, query) { f, q -> RegexQuery(f, q.value) }
    override fun transformDateRangeQuery(query: DateRangeQuery) = mapExpr(query.field, query) { f, q -> DateRangeQuery(f, q.lt, q.lte, q.gt, q.gte) }
    override fun transformRangeQuery(query: RangeQuery) = mapExpr(query.field, query) { f, q -> RangeQuery(f, q.lt, q.lte, q.gt, q.gte) }
    override fun transformBboxQuery(query: BboxQuery) = mapExpr(query.field, query) { f, q -> BboxQuery(f, q.value) }
    override fun transformExistsQuery(query: ExistsQuery) = mapExpr(query.field, query) { f, q -> ExistsQuery(f, q.value) }
}
