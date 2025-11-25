package io.em2m.search.core.daos

import com.fasterxml.jackson.databind.ObjectMapper
import io.em2m.obj.OperationPrecedence
import io.em2m.obj.OperationType
import io.em2m.search.core.model.*
import io.em2m.utils.Coerce

open class MultiCatchingStreamableSyncDao<T, DAO>(vararg delegates: DAO?,
                                                  objectMapper: ObjectMapper = Coerce.objectMapper,
                                                  debug: Boolean = false)
    : MultiCatchingSyncDao<T, DAO>(delegates=delegates, objectMapper=objectMapper, debug=debug),
        StreamableDao<T>  where DAO : AbstractSyncDao<T> , DAO : StreamableDao<T> {

    override fun streamRows(
        fields: List<Field>,
        query: Query,
        sorts: List<DocSort>,
        params: Map<String, Any>
    ): Iterator<List<Any?>> {
        val request = StreamRowsRequest(fields, query, sorts, params)

        val operation = Operation<StreamRowsRequest, Iterator<List<Any?>>>(
            OperationType.SEARCH,
            OperationPrecedence.ANY,
            { _, _ -> Result.success(true) },
            { delegate, param ->
                delegate.streamRows(param)
            }
        )

        return operation(request, debug=debug) ?: emptyList<List<Any>>().iterator()
    }

    override fun streamItems(
        query: Query,
        sorts: List<DocSort>,
        params: Map<String, Any>
    ): Iterator<T> {
        val request = StreamItemsRequest(query, sorts, params)

        val operation = Operation<StreamItemsRequest, Iterator<T>>(
            OperationType.SEARCH,
            OperationPrecedence.ANY,
            { _, _ -> Result.success(true) },
            { delegate, param ->
                delegate.streamItems(param)
            }
        )

        return operation(request, debug=debug) ?: emptyList<T>().iterator()
    }

}
