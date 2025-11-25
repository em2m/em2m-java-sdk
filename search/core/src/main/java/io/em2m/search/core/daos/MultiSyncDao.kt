package io.em2m.search.core.daos

import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.utils.Coerce


class MultiSyncDao<T, DAO>(vararg delegates: DAO,
                      objectMapper: ObjectMapper = Coerce.objectMapper,
                      debug: Boolean = false)
    : MultiCatchingSyncDao<T, DAO>(
        delegates = delegates,
        objectMapper = objectMapper, debug = debug) where DAO : AbstractSyncDao<T>

