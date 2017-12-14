package io.em2m.search.core.xform

import io.em2m.search.core.model.*

class FieldAliasQueryTransformer(val aliases: Map<String, Field>) : QueryTransformer() {

    fun applyAlias(field: String): String = aliases.getOrElse(field, { null })?.name ?: field

    override fun transformTermQuery(query: TermQuery) = TermQuery(applyAlias(query.field), query.value)
    override fun transformMatchQuery(query: MatchQuery) = MatchQuery(applyAlias(query.field), query.value, query.operator)
    override fun transformPhraseQuery(query: PhraseQuery) = PhraseQuery(applyAlias(query.field), query.value)
    override fun transformPrefixQuery(query: PrefixQuery) = PrefixQuery(applyAlias(query.field), query.value)
    override fun transformRegexQuery(query: RegexQuery) = RegexQuery(applyAlias(query.field), query.value)
    override fun transformRangeQuery(query: RangeQuery) = RangeQuery(applyAlias(query.field), query.lt, query.lte, query.gt, query.gte)
    override fun transformBboxQuery(query: BboxQuery): BboxQuery = BboxQuery(applyAlias(query.field), query.value)
    override fun transformExistsQuery(query: ExistsQuery): ExistsQuery = ExistsQuery(applyAlias(query.field), query.value)
}
