package io.em2m.search.core.model

import io.em2m.simplex.model.ExprContext


open class SearchExprContext(val map: Map<String, Any?>) {
    val request: SearchRequest by map
    val scope: Map<String, Any?> by map
}

class RowContext(exprContext: ExprContext) : SearchExprContext(exprContext) {
    val fieldValues: Map<String, Any?> by exprContext

    constructor(request: SearchRequest, scope: Map<String, Any?>, fieldValues: Map<String, Any?>)
            : this(mapOf(
            "request" to request,
            "scope" to scope,
            "fieldValues" to fieldValues))
}

class BucketContext(exprContext: ExprContext) : SearchExprContext(exprContext) {
    val bucket: Bucket by exprContext

    constructor(request: SearchRequest, scope: Map<String, Any?>, bucket: Bucket)
            : this(mapOf(
            "request" to request,
            "scope" to scope,
            "bucket" to bucket))
}
