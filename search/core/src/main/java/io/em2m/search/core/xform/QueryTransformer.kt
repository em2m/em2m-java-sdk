package io.em2m.search.core.xform

import io.em2m.search.core.model.*

open class QueryTransformer {

    fun transform(query: Query): Query = when (query) {

        is AndQuery -> {
            transformAndQuery(query)
        }
        is OrQuery -> {
            transformOrQuery(query)
        }
        is NotQuery -> {
            transformNotQuery(query)
        }
        is TermQuery -> {
            transformTermQuery(query)
        }
        is TermsQuery -> {
            transformTermsQuery(query)
        }
        is MatchQuery -> {
            transformMatchQuery(query)
        }
        is PhraseQuery -> {
            transformPhraseQuery(query)
        }
        is PrefixQuery -> {
            transformPrefixQuery(query)
        }
        is WildcardQuery -> {
            transformWildcardQuery(query)
        }
        is RegexQuery -> {
            transformRegexQuery(query)
        }
        is RangeQuery -> {
            transformRangeQuery(query)
        }
        is DateRangeQuery -> {
            transformDateRangeQuery(query)
        }
        is BboxQuery -> {
            transformBboxQuery(query)
        }
        is MatchAllQuery -> {
            transformMatchAllQuery(query)
        }
        is NativeQuery -> {
            transformNativeQuery(query)
        }
        is NamedQuery -> {
            transformNamedQuery(query)
        }
        is LuceneQuery -> {
            transformLuceneQuery(query)
        }
        is ExistsQuery -> {
            transformExistsQuery(query)
        }
        else -> {
            query
        }
    }

    // Boolean Queries
    open fun transformAndQuery(query: AndQuery): Query {
        return AndQuery(query.of.map { transform(it) })
    }

    open fun transformOrQuery(query: OrQuery): Query {
        return OrQuery(query.of.map { transform(it) })
    }

    open fun transformNotQuery(query: NotQuery): Query {
        return NotQuery(query.of.map { transform(it) })
    }

    // Field Queries

    open fun transformTermQuery(query: TermQuery): Query = query
    open fun transformTermsQuery(query: TermsQuery): Query = query
    open fun transformMatchQuery(query: MatchQuery): Query = query
    open fun transformPhraseQuery(query: PhraseQuery): Query = query
    open fun transformPrefixQuery(query: PrefixQuery): Query = query
    open fun transformWildcardQuery(query: WildcardQuery): Query = query
    open fun transformRegexQuery(query: RegexQuery): Query = query
    open fun transformDateRangeQuery(query: DateRangeQuery): Query = query
    open fun transformRangeQuery(query: RangeQuery): Query = query
    open fun transformBboxQuery(query: BboxQuery): Query = query
    open fun transformExistsQuery(query: ExistsQuery): Query = query

    // Special Queries

    open fun transformLuceneQuery(query: LuceneQuery): Query = query
    open fun transformMatchAllQuery(query: MatchAllQuery): Query = query
    open fun transformNativeQuery(query: NativeQuery): Query = query
    open fun transformNamedQuery(query: NamedQuery): Query = query


}