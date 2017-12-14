package io.em2m.search.core.xform

import io.em2m.search.core.model.AggResult
import io.em2m.search.core.model.Bucket
import io.em2m.search.core.model.Stats


open class AggResultTransformer {

    open fun transform(aggResult: AggResult): AggResult {
        val buckets = aggResult.buckets?.map({ transformBucket(it) })
        val stats = transformStats(aggResult.stats)
        val value = transformValue(aggResult.value)

        return AggResult(aggResult.key, buckets, stats, value)
        return aggResult
    }

    open fun transformBucket(bucket: Bucket): Bucket {
        return bucket
    }

    open fun transformStats(stats: Stats?): Stats? {
        return stats
    }

    open fun transformValue(value: Any?): Any? {
        return value
    }

}