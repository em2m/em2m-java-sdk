package io.em2m.search.migrate

import io.em2m.search.es.EsHit
import io.em2m.search.es.EsHits
import io.em2m.search.es.EsSearchResult
import io.em2m.search.es.EsShards
import io.em2m.search.es2.models.Es2IndexSettings
import io.em2m.search.es2.models.Es2MappingSettings
import io.em2m.search.es2.models.Es2Settings
import io.em2m.search.es8.models.Es8IndexSettings
import io.em2m.search.es8.models.Es8MappingSettings
import io.em2m.search.es8.models.Es8Settings
import io.em2m.search.es8.models.Es8Shards
import io.em2m.search.es8.models.search.Es8Hit
import io.em2m.search.es8.models.search.Es8Hits
import io.em2m.search.es8.models.search.Es8SearchResult
import io.em2m.search.es8.models.search.Es8Total
import io.em2m.utils.coerceNonNull

// settings

fun Es2Settings.toModern(): Es8Settings {
    return Es8Settings(Es8IndexSettings(
        creationDate = this.index.creationDate,
        numberOfShards = this.index.numberOfShards,
        numberOfReplicas = this.index.numberOfReplicas,
        mapping = Es8MappingSettings(
            ignoreMalformed = this.index.mapping.ignoreMalformed
        )
    ))
}

fun Es2Settings.Companion.fromModern(settings: Es8Settings): Es2Settings = settings.toLegacy()

fun Es8Settings.toLegacy(): Es2Settings {
    return Es2Settings(Es2IndexSettings(
        creationDate = this.index.creationDate,
        numberOfShards = this.index.numberOfShards,
        numberOfReplicas = this.index.numberOfReplicas,
        mapping = Es2MappingSettings(
            ignoreMalformed = this.index.mapping.ignoreMalformed
        )
    ))
}

fun Es8Settings.Companion.fromLegacy(settings: Es2Settings): Es8Settings = settings.toModern()

// shards

@Deprecated("Skipped cannot be inferred, this can give unexpected behavior.")
fun EsShards.toModern(): Es8Shards {
    return Es8Shards(total = total, successful= successful, failed = failed, skipped = 0)
}

fun EsShards.Companion.fromModern(shards: Es8Shards): EsShards = shards.toLegacy()

fun Es8Shards.toLegacy(): EsShards = EsShards(total, successful, failed)


// hits

fun EsHit.toModern(): Es8Hit {
    return Es8Hit(
        index = this.index,
        id = this.id,
        score = this.score,
        source = this.source
    )
}

fun EsHits.toModern(): Es8Hits {
    return Es8Hits(_total = Es8Total(value= this.total), hits = this.hits.map(EsHit::toModern), maxScore = this.max_score)
}

@Deprecated("Types cannot be mapped correctly, this can give unexpected behavior.")
fun Es8Hit.toLegacy(): EsHit = EsHit(index=index, type="null", id=id, score=score, source=source)

@Deprecated("Types cannot be mapped correctly, this can give unexpected behavior.")
fun Es8Hits.toLegacy(): EsHits {
    return EsHits(
        total= this.total,
        max_score = maxScore.coerceNonNull(0.0),
        hits = this.hits.map(Es8Hit::toLegacy))
}

fun Es8SearchResult.toLegacy(): EsSearchResult {
    return EsSearchResult(
        took= took,
        timedOut = timedOut,
        shards= shards.toLegacy(),
        scrollId = scrollId,
        hits= hits.toLegacy())
}

fun EsSearchResult.toModern(): Es8SearchResult {
    return Es8SearchResult(
        took= took,
        timedOut = timedOut,
        shards= shards.toModern(),
        scrollId = scrollId,
        hits= hits.toModern()
    )
}
