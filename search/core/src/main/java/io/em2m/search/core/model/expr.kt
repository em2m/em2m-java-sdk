package io.em2m.search.core.model

open class SearchExprContext(val request: SearchRequest?, val scope: Map<String, Any?>?) {
    open fun toMap(): Map<String, Any?> {
        return mapOf("request" to request, "scope" to scope)
    }
}

class RowContext(request: SearchRequest, scope: Map<String, Any?>, val fieldValues: Map<String, Any?>) : SearchExprContext(request, scope) {

    constructor(context: Map<String, Any?>) : this(
            context["request"] as? SearchRequest ?: SearchRequest(),
            context["scope"] as? Map<String, Any?> ?: emptyMap(),
            context["fieldValues"] as? Map<String, Any?> ?: emptyMap())

    override fun toMap(): Map<String, Any?> {
        return (scope ?: emptyMap()).plus(listOf("request" to request, "scope" to scope, "fieldValues" to fieldValues))
    }
}

class BucketContext(request: SearchRequest, scope: Map<String, Any?>, val bucket: Bucket) : SearchExprContext(request, scope) {

    constructor(context: Map<String, Any?>) : this(
            context["request"] as? SearchRequest ?: SearchRequest(),
            context["scope"] as? Map<String, Any?> ?: emptyMap(),
            context["bucket"] as? Bucket ?: Bucket(null, 0))

    override fun toMap(): Map<String, Any?> {
        return mapOf("request" to request, "scope" to scope, "bucket" to bucket)
    }

}

