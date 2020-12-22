package io.em2m.search.core.xform

import io.em2m.search.core.model.*
import io.em2m.simplex.model.ExprContext

@Deprecated("Use FieldTransformerDao instead")
class FieldAliasQueryTransformer(private val aliases: Map<String, Field>) : QueryTransformer() {

    private fun applyAlias(field: String): String {
        val alias = aliases[field]
        return /*alias?.expr ?:*/ alias?.name ?: field
    }

    override fun transformTermQuery(query: TermQuery, context: ExprContext) = TermQuery(applyAlias(query.field), query.value)
    override fun transformTermsQuery(query: TermsQuery, context: ExprContext) = TermsQuery(applyAlias(query.field), query.value)
    override fun transformMatchQuery(query: MatchQuery, context: ExprContext) = MatchQuery(applyAlias(query.field), query.value, query.operator)
    override fun transformPhraseQuery(query: PhraseQuery, context: ExprContext) = PhraseQuery(applyAlias(query.field), query.value)
    override fun transformPrefixQuery(query: PrefixQuery, context: ExprContext) = PrefixQuery(applyAlias(query.field), query.value)
    override fun transformWildcardQuery(query: WildcardQuery, context: ExprContext) = WildcardQuery(applyAlias(query.field), query.value)
    override fun transformRegexQuery(query: RegexQuery, context: ExprContext) = RegexQuery(applyAlias(query.field), query.value)
    override fun transformDateRangeQuery(query: DateRangeQuery, context: ExprContext) = DateRangeQuery(applyAlias(query.field), query.lt, query.lte, query.gt, query.gte)
    override fun transformRangeQuery(query: RangeQuery, context: ExprContext) = RangeQuery(applyAlias(query.field), query.lt, query.lte, query.gt, query.gte)
    override fun transformBboxQuery(query: BboxQuery, context: ExprContext): BboxQuery = BboxQuery(applyAlias(query.field), query.value)
    override fun transformExistsQuery(query: ExistsQuery, context: ExprContext): ExistsQuery = ExistsQuery(applyAlias(query.field), query.value)
}
